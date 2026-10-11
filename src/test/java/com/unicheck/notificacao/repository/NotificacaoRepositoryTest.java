package com.unicheck.notificacao.repository;

import com.unicheck.checklist.domain.Checklist;
import com.unicheck.checklist.domain.ChecklistConcluido;
import com.unicheck.checklist.domain.ItemChecklist;
import com.unicheck.checklist.domain.ProgressoItem;
import com.unicheck.checklist.enumeration.StatusChecklist;
import com.unicheck.checklist.enumeration.StatusProgressoItem;
import com.unicheck.notificacao.enumeration.TipoNotificacao;
import com.unicheck.usuario.domain.Aluno;
import com.unicheck.usuario.domain.Usuario;
import com.unicheck.usuario.enumeration.RoleUsuario;
import com.unicheck.usuario.enumeration.StatusMatricula;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.PageRequest;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @apiNote Exercita contra o Postgres o SQL que decide quem recebe cada notificação: o INSERT para
 *          todos os alunos, a rotina de checklist parado (3 dias, repetição a cada 7, recomeço ao
 *          avançar), as preferências, a remoção por objeto e o cascade ao excluir o aluno.
 *
 *          A rotina é "executada" em datas diferentes passando o instante, sem esperar dias. Como o
 *          banco de desenvolvimento pode ter outros alunos, as contagens olham só os usuários do
 *          fixture. Cada teste roda em transação revertida, e os fixtures usam o prefixo "zzt-".
 *          A classe é ignorada quando não há Postgres na porta esperada.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnabledIf("bancoDisponivel")
class NotificacaoRepositoryTest {
    // Ultima etapa concluida na noite do dia 1; os dias da rotina contam por data do calendario.
    private static final LocalDateTime ULTIMA_ETAPA = LocalDateTime.of(2026, 10, 1, 18, 0);

    @Autowired NotificacaoRepository repository;
    @Autowired TestEntityManager em;

    private Usuario alunoUsuario;
    private Aluno aluno;
    private Usuario outroAlunoUsuario;
    private Aluno outroAluno;
    private Usuario admin;
    private Checklist checklist;
    private ItemChecklist etapa1;
    private ItemChecklist etapa2;

    static boolean bancoDisponivel() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", 5432), 500);
            return true;
        } catch (Exception naoDisponivel) {
            return false;
        }
    }

    @BeforeEach
    void semear() {
        alunoUsuario = em.persist(usuario("zzt-n-aluno", RoleUsuario.ALUNO));
        aluno = em.persist(aluno(alunoUsuario));
        outroAlunoUsuario = em.persist(usuario("zzt-n-outro", RoleUsuario.ALUNO));
        outroAluno = em.persist(aluno(outroAlunoUsuario));
        admin = em.persist(usuario("zzt-n-admin", RoleUsuario.ADMIN));
        checklist = em.persist(checklist("zzt-Primeiros acessos", StatusChecklist.PUBLICADO));
        etapa1 = em.persist(etapa(checklist, 1));
        etapa2 = em.persist(etapa(checklist, 2));
        em.flush();
    }

    // ---------------------------------------------------------------- conteúdo novo

    @Test
    void notificarAlunosGravaUmaParaCadaAlunoENenhumaParaAdmin() {
        repository.notificarAlunos("BENEFICIO_NOVO", 500L, "Novo benefício: Bolsa");

        assertEquals(1, contar(alunoUsuario, TipoNotificacao.BENEFICIO_NOVO));
        assertEquals(1, contar(outroAlunoUsuario, TipoNotificacao.BENEFICIO_NOVO));
        assertEquals(0, contar(admin, TipoNotificacao.BENEFICIO_NOVO));
        var gravada = repository.findByUsuarioRaOrderByCriadoEmDescIdDesc("zzt-n-aluno", PageRequest.of(0, 5)).getContent();
        assertEquals("Novo benefício: Bolsa", gravada.get(0).getMensagem());
        assertEquals(500L, gravada.get(0).getReferenciaId());
        assertFalse(gravada.get(0).getLida());
    }

    @Test
    void notificarAlunosPulaQuemDesligouOTipo() {
        repository.desativar("zzt-n-aluno", "MANUAL_NOVO");

        repository.notificarAlunos("MANUAL_NOVO", 501L, "Novo manual: Portal");
        repository.notificarAlunos("BENEFICIO_NOVO", 502L, "Novo benefício: Bolsa");

        assertEquals(0, contar(alunoUsuario, TipoNotificacao.MANUAL_NOVO));
        assertEquals(1, contar(outroAlunoUsuario, TipoNotificacao.MANUAL_NOVO));
        // Desligar um tipo nao afeta os outros.
        assertEquals(1, contar(alunoUsuario, TipoNotificacao.BENEFICIO_NOVO));
    }

    // ---------------------------------------------------------------- checklist parado

    @Test
    void primeiroLembreteSaiAosTresDiasEDepoisACadaSete() {
        concluir(aluno, etapa1, ULTIMA_ETAPA);

        assertEquals(0, rotinaNoDia(3));  // dia 3: so 2 dias desde a ultima etapa
        assertEquals(1, rotinaNoDia(4));  // dia 4: completou 3 dias
        assertEquals(0, rotinaNoDia(5));
        assertEquals(0, rotinaNoDia(10)); // 6 dias depois do lembrete
        assertEquals(1, rotinaNoDia(11)); // 7 dias depois do lembrete
        assertEquals(0, rotinaNoDia(12));
        assertEquals(1, rotinaNoDia(18));
        assertEquals(3, contar(alunoUsuario, TipoNotificacao.CHECKLIST_PARADO));
    }

    @Test
    void lembreteApontaParaOChecklistEContaOsDias() {
        concluir(aluno, etapa1, ULTIMA_ETAPA);

        rotinaNoDia(4);

        var lembrete = repository.findByUsuarioRaOrderByCriadoEmDescIdDesc("zzt-n-aluno", PageRequest.of(0, 5)).getContent().get(0);
        assertEquals(TipoNotificacao.CHECKLIST_PARADO, lembrete.getTipo());
        assertEquals(checklist.getId(), lembrete.getReferenciaId());
        assertEquals("Você não avança no checklist zzt-Primeiros acessos há 3 dias.", lembrete.getMensagem());
    }

    @Test
    void avancarRecomecaAContagemMesmoAntesDosSeteDias() {
        concluir(aluno, etapa1, ULTIMA_ETAPA);
        assertEquals(1, rotinaNoDia(4));

        // Concluiu outra etapa no dia 6: o proximo lembrete conta 3 dias a partir dai, nao 7 do anterior.
        concluir(aluno, etapa2, ULTIMA_ETAPA.withDayOfMonth(6));

        assertEquals(0, rotinaNoDia(8));
        assertEquals(1, rotinaNoDia(9));
    }

    @Test
    void checklistConcluidoNaoGeraLembrete() {
        concluir(aluno, etapa1, ULTIMA_ETAPA);
        concluir(aluno, etapa2, ULTIMA_ETAPA);
        em.persist(new ChecklistConcluido(aluno, checklist, null, offset(ULTIMA_ETAPA)));
        em.flush();

        assertEquals(0, rotinaNoDia(10));
    }

    @Test
    void checklistNaoPublicadoNaoGeraLembrete() {
        Checklist arquivado = em.persist(checklist("zzt-Arquivado", StatusChecklist.ARQUIVADO));
        ItemChecklist etapaArquivada = em.persist(etapa(arquivado, 1));
        em.persist(etapa(arquivado, 2));
        concluir(aluno, etapaArquivada, ULTIMA_ETAPA);

        assertEquals(0, rotinaNoDia(10));
    }

    @Test
    void checklistNaoIniciadoNaoGeraLembrete() {
        // O fixture tem um checklist publicado, mas nenhum aluno concluiu etapa nele.
        assertEquals(0, rotinaNoDia(30));
    }

    @Test
    void quemDesligouOLembreteNaoRecebeEOsOutrosSim() {
        concluir(aluno, etapa1, ULTIMA_ETAPA);
        concluir(outroAluno, etapa1, ULTIMA_ETAPA);
        repository.desativar("zzt-n-aluno", "CHECKLIST_PARADO");

        repository.notificarChecklistsParados(diaDoMes(4), 3, 7);

        assertEquals(0, contar(alunoUsuario, TipoNotificacao.CHECKLIST_PARADO));
        assertEquals(1, contar(outroAlunoUsuario, TipoNotificacao.CHECKLIST_PARADO));
    }

    // ---------------------------------------------------------------- leitura e lida

    @Test
    void marcarComoLidaSoFuncionaParaODono() {
        repository.notificarAlunos("BENEFICIO_NOVO", 503L, "Novo benefício: Bolsa");
        Long idDoAluno = repository.findByUsuarioRaOrderByCriadoEmDescIdDesc("zzt-n-aluno", PageRequest.of(0, 5))
                .getContent().get(0).getId();

        assertEquals(0, repository.marcarComoLida(idDoAluno, "zzt-n-outro"));
        assertEquals(1, repository.marcarComoLida(idDoAluno, "zzt-n-aluno"));
        assertEquals(0, repository.countByUsuarioRaAndLidaFalse("zzt-n-aluno"));
        assertEquals(1, repository.countByUsuarioRaAndLidaFalse("zzt-n-outro"));
    }

    @Test
    void marcarTodasComoLidasAfetaSoOAluno() {
        repository.notificarAlunos("BENEFICIO_NOVO", 504L, "Novo benefício: A");
        repository.notificarAlunos("MANUAL_NOVO", 505L, "Novo manual: B");

        assertEquals(2, repository.marcarTodasComoLidas("zzt-n-aluno"));

        assertEquals(0, repository.countByUsuarioRaAndLidaFalse("zzt-n-aluno"));
        assertEquals(2, repository.countByUsuarioRaAndLidaFalse("zzt-n-outro"));
        assertEquals(2, repository.findByUsuarioRaAndLidaOrderByCriadoEmDescIdDesc("zzt-n-aluno", true, PageRequest.of(0, 5)).getTotalElements());
        assertEquals(0, repository.findByUsuarioRaAndLidaOrderByCriadoEmDescIdDesc("zzt-n-aluno", false, PageRequest.of(0, 5)).getTotalElements());
    }

    // ---------------------------------------------------------------- exclusão e preferências

    @Test
    void removerDoObjetoApagaSoOsTiposEOIdInformados() {
        repository.notificarAlunos("CHECKLIST_NOVO", checklist.getId(), "Novo checklist: X");
        repository.notificarAlunos("MANUAL_NOVO", checklist.getId(), "Novo manual: mesmo id, outro tipo");
        concluir(aluno, etapa1, ULTIMA_ETAPA);
        repository.notificarChecklistsParados(diaDoMes(4), 3, 7);

        repository.removerDoObjeto(List.of(TipoNotificacao.CHECKLIST_NOVO, TipoNotificacao.CHECKLIST_PARADO), checklist.getId());

        assertEquals(0, contar(alunoUsuario, TipoNotificacao.CHECKLIST_NOVO));
        assertEquals(0, contar(alunoUsuario, TipoNotificacao.CHECKLIST_PARADO));
        assertEquals(1, contar(alunoUsuario, TipoNotificacao.MANUAL_NOVO));
    }

    @Test
    void excluirOAlunoApagaAsNotificacoesEAsPreferenciasDele() {
        repository.notificarAlunos("BENEFICIO_NOVO", 506L, "Novo benefício: Bolsa");
        repository.desativar("zzt-n-aluno", "MANUAL_NOVO");
        em.clear();

        // Sem o ON DELETE CASCADE da V18, apagar o usuario falharia por causa das notificacoes.
        em.getEntityManager().createNativeQuery("DELETE FROM tb_aluno WHERE id = :id").setParameter("id", aluno.getId()).executeUpdate();
        em.getEntityManager().createNativeQuery("DELETE FROM tb_usuario WHERE id = :id").setParameter("id", alunoUsuario.getId()).executeUpdate();

        assertEquals(0, contar(alunoUsuario, TipoNotificacao.BENEFICIO_NOVO));
        assertEquals(0L, ((Number) em.getEntityManager()
                .createNativeQuery("SELECT count(*) FROM tb_notificacao_desativada WHERE usuario_id = :id")
                .setParameter("id", alunoUsuario.getId()).getSingleResult()).longValue());
    }

    @Test
    void desligarDuasVezesGuardaUmaLinhaELigarRemove() {
        assertEquals(1, repository.desativar("zzt-n-aluno", "MANUAL_NOVO"));
        assertEquals(0, repository.desativar("zzt-n-aluno", "MANUAL_NOVO"));
        assertEquals(List.of("MANUAL_NOVO"), repository.tiposDesativados("zzt-n-aluno"));

        assertEquals(1, repository.ativar("zzt-n-aluno", "MANUAL_NOVO"));
        assertEquals(0, repository.ativar("zzt-n-aluno", "MANUAL_NOVO"));
        assertTrue(repository.tiposDesativados("zzt-n-aluno").isEmpty());
    }

    // ---------------------------------------------------------------- retenção

    @Test
    void apagarAntigasRespeitaOsPrazosDeLidasENaoLidas() {
        LocalDateTime agora = LocalDateTime.of(2026, 12, 1, 3, 0);
        Long lida31 = notificacaoCom(601L, true, agora.minusDays(31));
        Long lida29 = notificacaoCom(602L, true, agora.minusDays(29));
        Long naoLida91 = notificacaoCom(603L, false, agora.minusDays(91));
        Long naoLida89 = notificacaoCom(604L, false, agora.minusDays(89));

        repository.apagarAntigas(agora, 30, 90);

        assertFalse(existe(lida31), "lida com mais de 30 dias deveria ter sido apagada");
        assertTrue(existe(lida29), "lida com menos de 30 dias deveria continuar");
        assertFalse(existe(naoLida91), "nao lida com mais de 90 dias deveria ter sido apagada");
        assertTrue(existe(naoLida89), "nao lida com menos de 90 dias deveria continuar");
    }

    @Test
    void naoLidaComMaisDeTrintaDiasContinuaAteNoventa() {
        LocalDateTime agora = LocalDateTime.of(2026, 12, 1, 3, 0);
        Long naoLida45 = notificacaoCom(605L, false, agora.minusDays(45));

        repository.apagarAntigas(agora, 30, 90);

        // O prazo de 30 dias vale so para as lidas.
        assertTrue(existe(naoLida45));
    }

    // ---------------------------------------------------------------- auxiliares

    /** Grava uma notificacao do aluno e ajusta lida e criado_em direto no banco, ja que o @PrePersist fixa os dois. */
    private Long notificacaoCom(Long referenciaId, boolean lida, LocalDateTime criadoEm) {
        repository.notificarAlunos("BENEFICIO_NOVO", referenciaId, "Novo benefício: retencao");
        Long id = ((Number) em.getEntityManager()
                .createNativeQuery("SELECT id FROM tb_notificacao WHERE usuario_id = :u AND referencia_id = :r")
                .setParameter("u", alunoUsuario.getId()).setParameter("r", referenciaId)
                .getSingleResult()).longValue();
        em.getEntityManager().createNativeQuery("UPDATE tb_notificacao SET lida = :l, criado_em = :c WHERE id = :id")
                .setParameter("l", lida).setParameter("c", criadoEm).setParameter("id", id)
                .executeUpdate();
        return id;
    }

    private boolean existe(Long notificacaoId) {
        return ((Number) em.getEntityManager()
                .createNativeQuery("SELECT count(*) FROM tb_notificacao WHERE id = :id")
                .setParameter("id", notificacaoId).getSingleResult()).longValue() == 1;
    }

    /** Roda a rotina às 9h do dia informado e devolve quantos lembretes o aluno do fixture ganhou. */
    private long rotinaNoDia(int dia) {
        long antes = contar(alunoUsuario, TipoNotificacao.CHECKLIST_PARADO);
        repository.notificarChecklistsParados(diaDoMes(dia), 3, 7);
        return contar(alunoUsuario, TipoNotificacao.CHECKLIST_PARADO) - antes;
    }

    private LocalDateTime diaDoMes(int dia) {
        return LocalDateTime.of(2026, 10, 1, 9, 0).plusDays(dia - 1);
    }

    private long contar(Usuario usuario, TipoNotificacao tipo) {
        return ((Number) em.getEntityManager()
                .createNativeQuery("SELECT count(*) FROM tb_notificacao WHERE usuario_id = :u AND tipo = :t")
                .setParameter("u", usuario.getId()).setParameter("t", tipo.name())
                .getSingleResult()).longValue();
    }

    private void concluir(Aluno quem, ItemChecklist item, LocalDateTime quando) {
        ProgressoItem progresso = new ProgressoItem(quem, item, StatusProgressoItem.CONCLUIDO, offset(quando));
        progresso.setXpCreditado(item.getXp());
        em.persist(progresso);
        em.flush();
    }

    // As colunas de data sao TIMESTAMP sem fuso, gravadas no horario local da aplicacao.
    private OffsetDateTime offset(LocalDateTime local) {
        return local.atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }

    private Usuario usuario(String ra, RoleUsuario role) {
        Usuario usuario = new Usuario();
        usuario.setNome("Teste " + ra);
        usuario.setRa(ra);
        usuario.setSenha("hash-qualquer");
        usuario.setDataNascimento(LocalDate.of(2000, 1, 1));
        usuario.setRole(role);
        usuario.setEmail(ra + "@example.invalid");
        return usuario;
    }

    private Aluno aluno(Usuario usuario) {
        Aluno aluno = new Aluno();
        aluno.setUsuario(usuario);
        aluno.setStatus(StatusMatricula.ATIVA);
        aluno.setPeriodo("1");
        aluno.setXpTotal(0);
        return aluno;
    }

    private Checklist checklist(String nome, StatusChecklist status) {
        Checklist c = new Checklist();
        c.setNome(nome);
        c.setDescricao("Fixture de notificacao");
        c.setOrdem(9000);
        c.setXp(0);
        c.setHorasComplementares(0);
        c.setStatus(status);
        return c;
    }

    private ItemChecklist etapa(Checklist c, int ordem) {
        ItemChecklist i = new ItemChecklist();
        i.setChecklist(c);
        i.setTitulo("Etapa " + ordem);
        i.setDescricao("Resumo");
        i.setConteudo("Conteudo");
        i.setOrdem(ordem);
        i.setXp(10);
        return i;
    }
}
