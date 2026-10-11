package com.unicheck.manual.service;

import com.unicheck.manual.domain.Manual;
import com.unicheck.manual.dto.AtalhoManualDTO;
import com.unicheck.manual.dto.ManualRequestDTO;
import com.unicheck.manual.dto.ManualResponseDTO;
import com.unicheck.manual.enumeration.CategoriaManual;
import com.unicheck.manual.repository.AtalhoManualRepository;
import com.unicheck.manual.repository.ManualRepository;
import com.unicheck.notificacao.service.NotificacaoService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @apiNote Roda a ManualService de verdade, sem mock, contra o Postgres.
 *
 *          O teste unitário confere que a Service chama deslocarOrdem com os números certos; este
 *          confere o resultado no banco: a sequência sem buraco e sem ordem repetida, a remoção
 *          limpando relacionados e atalhos, e a troca de atalhos desfeita inteira quando falha.
 *
 *          Ao fim de cada operação, SET CONSTRAINTS ALL IMMEDIATE força a checagem das constraints
 *          deferidas, que normalmente só aconteceria no commit, e o contexto é limpo para que a
 *          leitura venha do banco e não da memória. Todo fixture usa o prefixo "zzt-" e a ordem
 *          parte da maior já existente. A classe é ignorada quando não há Postgres na porta do
 *          application.properties.
 * @author Mateus Alves Costa
 * @since 27.09.2026
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ManualService.class, NotificacaoService.class})
@EnabledIf("bancoDisponivel")
class ManualServiceIntegracaoTest {
    @Autowired ManualService service;
    @Autowired ManualRepository manualRepository;
    @Autowired AtalhoManualRepository atalhoRepository;
    @Autowired TestEntityManager em;
    @Autowired PlatformTransactionManager transactionManager;

    private int base;

    static boolean bancoDisponivel() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", 5432), 500);
            return true;
        } catch (Exception naoDisponivel) {
            return false;
        }
    }

    @BeforeEach
    void lerBase() {
        base = manualRepository.maiorOrdem();
    }

    /**
     * @apiNote Sem ordem, os manuais entram em sequência no fim.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void cadastrosSemOrdemEntramEmSequencia() {
        criar("zzt-a", null);
        criar("zzt-b", null);
        criar("zzt-c", null);
        sincronizar();

        assertEquals(List.of("zzt-a", "zzt-b", "zzt-c"), sequencia());
    }

    /**
     * @apiNote Cadastrar no meio empurra os seguintes, sem buraco e sem ordem repetida.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void cadastroNoMeioEmpurraOsSeguintes() {
        criar("zzt-a", null);
        criar("zzt-c", null);
        criar("zzt-b", base + 2);
        sincronizar();

        assertEquals(List.of("zzt-a", "zzt-b", "zzt-c"), sequencia());
        assertSequenciaContinua();
    }

    /**
     * @apiNote Mover para cima e para baixo reorganiza os vizinhos.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void moverParaCimaEParaBaixoReorganizaOsVizinhos() {
        criar("zzt-a", null);
        criar("zzt-b", null);
        criar("zzt-c", null);
        ManualResponseDTO d = criar("zzt-d", null);
        sincronizar();

        service.update(d.id(), request("zzt-d", base + 1, List.of()));
        sincronizar();
        assertEquals(List.of("zzt-d", "zzt-a", "zzt-b", "zzt-c"), sequencia());

        service.update(d.id(), request("zzt-d", base + 3, List.of()));
        sincronizar();
        assertEquals(List.of("zzt-a", "zzt-b", "zzt-d", "zzt-c"), sequencia());
        assertSequenciaContinua();
    }

    /**
     * @apiNote O rodapé da página segue a sequência depois de uma mudança de posição.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void anteriorEProximoAcompanhamAReordenacao() {
        ManualResponseDTO a = criar("zzt-a", null);
        ManualResponseDTO b = criar("zzt-b", null);
        criar("zzt-c", null);
        sincronizar();

        service.update(a.id(), request("zzt-a", base + 3, List.of()));
        sincronizar();

        ManualResponseDTO pagina = service.findById(a.id());
        assertEquals("zzt-c", pagina.anterior().titulo());
        assertNull(pagina.proximo());
        assertNull(service.findById(b.id()).anterior());
    }

    /**
     * @apiNote Reordenar os relacionados pela Service grava a nova ordem sem violar a constraint.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void reordenarRelacionadosPelaService() {
        ManualResponseDTO a = criar("zzt-a", null);
        ManualResponseDTO b = criar("zzt-b", null);
        ManualResponseDTO c = criar("zzt-c", null);
        service.update(a.id(), request("zzt-a", null, List.of(b.id(), c.id())));
        sincronizar();

        service.update(a.id(), request("zzt-a", null, List.of(c.id(), b.id())));
        sincronizar();

        assertEquals(List.of("zzt-c", "zzt-b"), service.findById(a.id()).relacionados().stream()
                .map(r -> r.titulo()).toList());
    }

    /**
     * @apiNote Remover fecha o buraco na sequência, tira o manual dos relacionados dos outros sem
     *          deixar posição vazia, apaga o atalho dele e limpa quem o indicava no próximo passo.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void removerLimpaTodasAsReferencias() {
        ManualResponseDTO a = criar("zzt-a", null);
        ManualResponseDTO b = criar("zzt-b", null);
        ManualResponseDTO c = criar("zzt-c", null);
        ManualRequestDTO aApontandoParaB = new ManualRequestDTO(null, CategoriaManual.PORTAL_FERRAMENTAS_DIGITAIS, 1,
                "zzt-a", "Resumo.", null, null, null, "Saber.", null, null, null, null, null, null, null, null,
                "Veja o b.", b.id(), null, List.of(b.id(), c.id()));
        service.update(a.id(), aApontandoParaB);
        service.substituirAtalhos(List.of(new AtalhoManualDTO(b.id(), "Atalho do b"),
                new AtalhoManualDTO(c.id(), "Atalho do c")));
        sincronizar();

        service.delete(b.id());
        sincronizar();

        assertEquals(List.of("zzt-a", "zzt-c"), sequencia());
        assertSequenciaContinua();
        ManualResponseDTO paginaA = service.findById(a.id());
        assertEquals(List.of("zzt-c"), paginaA.relacionados().stream().map(r -> r.titulo()).toList());
        assertNull(paginaA.proximoPassoManual());
        assertEquals(List.of(new AtalhoManualDTO(c.id(), "Atalho do c")), atalhosZzt());
    }

    /**
     * @apiNote Remover o manual que está no meio de uma lista de três relacionados mantém a lista
     *          compacta: o terceiro sobe para a segunda posição, sem null no lugar.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void removerRelacionadoDoMeioNaoDeixaNull() {
        ManualResponseDTO a = criar("zzt-a", null);
        ManualResponseDTO b = criar("zzt-b", null);
        ManualResponseDTO c = criar("zzt-c", null);
        ManualResponseDTO d = criar("zzt-d", null);
        service.update(a.id(), request("zzt-a", null, List.of(b.id(), c.id(), d.id())));
        sincronizar();

        service.delete(c.id());
        sincronizar();

        assertEquals(List.of("zzt-b", "zzt-d"), service.findById(a.id()).relacionados().stream()
                .map(r -> r.titulo()).toList());
    }

    /**
     * @apiNote Trocar os atalhos de novo substitui a lista inteira, na ordem enviada.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void substituirAtalhosDuasVezesFicaSoComASegunda() {
        ManualResponseDTO a = criar("zzt-a", null);
        ManualResponseDTO b = criar("zzt-b", null);
        service.substituirAtalhos(List.of(new AtalhoManualDTO(a.id(), "Primeiro"), new AtalhoManualDTO(b.id(), "Segundo")));
        sincronizar();

        service.substituirAtalhos(List.of(new AtalhoManualDTO(b.id(), "Agora primeiro")));
        sincronizar();

        assertEquals(List.of(new AtalhoManualDTO(b.id(), "Agora primeiro")), service.listarAtalhos());
    }

    /**
     * @apiNote Se a troca falha no meio, a remoção dos atalhos antigos também é desfeita.
     *
     *          Precisa de transação própria da Service: dentro da transação do teste o rollback não
     *          seria observável. Por isso o teste roda fora dela e limpa o que criou ao final.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void falhaNaTrocaDeAtalhosMantemOsAntigos() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        List<AtalhoManualDTO> antes = service.listarAtalhos();
        ManualResponseDTO a = tx.execute(status -> criar("zzt-rollback", null));
        try {
            List<AtalhoManualDTO> comA = new java.util.ArrayList<>(antes);
            comA.add(new AtalhoManualDTO(a.id(), "zzt-atalho"));
            service.substituirAtalhos(comA);

            assertThrows(EntityNotFoundException.class, () -> service.substituirAtalhos(List.of(
                    new AtalhoManualDTO(a.id(), "zzt-novo"), new AtalhoManualDTO(Long.MAX_VALUE, "zzt-inexistente"))));

            assertEquals(comA, service.listarAtalhos());
        } finally {
            service.substituirAtalhos(antes);
            service.delete(a.id());
        }
    }

    private ManualResponseDTO criar(String titulo, Integer ordem) {
        return service.create(request(titulo, ordem, List.of()));
    }

    private ManualRequestDTO request(String titulo, Integer ordem, List<Long> relacionadoIds) {
        return new ManualRequestDTO(ordem, CategoriaManual.PORTAL_FERRAMENTAS_DIGITAIS, 1, titulo, "Resumo.",
                null, null, null, "Saber.", null, null, null, null, null, null, null, null, null, null, null,
                relacionadoIds);
    }

    /**
     * @apiNote Grava o pendente, força a checagem das constraints deferidas e esvazia o contexto.
     */
    private void sincronizar() {
        em.flush();
        em.getEntityManager().createNativeQuery("SET CONSTRAINTS ALL IMMEDIATE").executeUpdate();
        em.getEntityManager().createNativeQuery("SET CONSTRAINTS ALL DEFERRED").executeUpdate();
        em.clear();
    }

    private List<Manual> manuaisZzt() {
        return manualRepository.findAll().stream().filter(m -> m.getTitulo().startsWith("zzt-"))
                .sorted(Comparator.comparing(Manual::getOrdem)).toList();
    }

    private List<String> sequencia() {
        return manuaisZzt().stream().map(Manual::getTitulo).toList();
    }

    private void assertSequenciaContinua() {
        List<Integer> ordens = manuaisZzt().stream().map(Manual::getOrdem).toList();
        for(int i = 0; i < ordens.size(); i++) assertEquals(base + i + 1, ordens.get(i));
    }

    private List<AtalhoManualDTO> atalhosZzt() {
        return service.listarAtalhos().stream().filter(a -> a.nome().startsWith("Atalho do")).toList();
    }
}
