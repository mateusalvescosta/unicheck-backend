package com.unicheck.manual.repository;

import com.unicheck.manual.domain.LocalAtendimento;
import com.unicheck.manual.domain.Manual;
import com.unicheck.manual.domain.PassoManual;
import com.unicheck.manual.enumeration.CategoriaManual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @apiNote Exercita o mapeamento de Manual e as consultas de ManualRepository contra o Postgres
 *          de verdade, com as tabelas criadas pela V16.
 *
 *          O teste unitário da Service mocka o repository, então os EXISTS da busca, a contagem da
 *          paginação, as colunas de ordem das coleções e a constraint única deferida de
 *          tb_manual.ordem não são cobertos por ele. Aqui eles rodam.
 *
 *          Cada teste roda em transação revertida, e todo fixture carrega o prefixo "zzt-" para que
 *          as buscas nunca alcancem dados que já estejam no banco. A ordem dos fixtures parte da
 *          maior ordem já existente, pelo mesmo motivo. A classe é ignorada quando não há Postgres
 *          na porta do application.properties.
 * @author Mateus Alves Costa
 * @since 27.09.2026
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnabledIf("bancoDisponivel")
class ManualRepositoryTest {
    @Autowired ManualRepository repository;
    @Autowired TestEntityManager em;

    private int base;
    private Manual alfa;
    private Manual beta;
    private Manual gama;

    static boolean bancoDisponivel() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", 5432), 500);
            return true;
        } catch (Exception naoDisponivel) {
            return false;
        }
    }

    /**
     * @apiNote Semeia três manuais em sequência que cobrem, entre si, todos os blocos alcançados
     *          pela busca.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @BeforeEach
    void semear() {
        base = repository.maiorOrdem();

        alfa = manual("zzt-alfa", base + 1, CategoriaManual.MATRICULA_VIDA_ACADEMICA);
        alfa.setResumo("resumo xilofone");
        alfa.getPassos().add(new PassoManual(2, "passo dois trombone"));
        alfa.getPassos().add(new PassoManual(1, "passo um"));
        alfa.getOndeResolver().add(new LocalAtendimento("zzt-local-saxofone", null));
        alfa.getAtencao().add("atencao violino");

        beta = manual("zzt-beta", base + 2, CategoriaManual.MATRICULA_VIDA_ACADEMICA);
        beta.getEntendaMelhor().add("entenda OBOÉ maiusculo");
        beta.getPrazoERegras().add("prazo fagote");
        beta.setAvisoAtualizacao("aviso clarinete");

        gama = manual("zzt-gama", base + 3, CategoriaManual.PORTAL_FERRAMENTAS_DIGITAIS);
        gama.getDetalhes().add("detalhe tuba");
        gama.getAntesDeComecar().add("requisito harpa");
        gama.setQuandoSeAplica("quando bandolim");

        em.persist(alfa);
        em.persist(beta);
        em.persist(gama);
        em.flush();
    }

    // ---------------------------------------------------------------- mapeamento

    /**
     * @apiNote Todos os blocos voltam do banco como foram gravados; os passos voltam na ordem
     *          numérica (OrderBy) e as listas de texto na ordem de inserção (OrderColumn).
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void gravaERelêTodosOsBlocos() {
        Manual completo = manual("zzt-completo", base + 4, CategoriaManual.UTILIDADES_INSTITUCIONAIS);
        completo.setLinkTexto("Abrir Portal");
        completo.setLinkUrl("https://portal");
        completo.setResultadoEsperado("Vínculo renovado.");
        completo.setProximoPassoDescricao("Veja o AVA.");
        completo.setProximoPassoManual(beta);
        completo.getAtencao().addAll(List.of("a1", "a2"));
        completo.getAntesDeComecar().addAll(List.of("r1", "r2"));
        completo.getPrazoERegras().add("p1");
        completo.getDetalhes().add("d1");
        completo.getEntendaMelhor().addAll(List.of("e2", "e1"));
        completo.getOndeResolver().add(new LocalAtendimento("Multiatendimento", "https://multi"));
        completo.getOndeResolver().add(new LocalAtendimento("Portal", null));
        completo.getRelacionados().addAll(List.of(gama, alfa));
        em.persist(completo);
        em.flush();
        em.clear();

        Manual lido = repository.findById(completo.getId()).orElseThrow();

        assertEquals("https://portal", lido.getLinkUrl());
        assertEquals("zzt-beta", lido.getProximoPassoManual().getTitulo());
        assertEquals(List.of("a1", "a2"), lido.getAtencao());
        assertEquals(List.of("r1", "r2"), lido.getAntesDeComecar());
        assertEquals(List.of("e2", "e1"), lido.getEntendaMelhor());
        assertEquals("Multiatendimento", lido.getOndeResolver().get(0).getNome());
        assertNull(lido.getOndeResolver().get(1).getUrl());
        assertEquals(List.of("zzt-gama", "zzt-alfa"), lido.getRelacionados().stream().map(Manual::getTitulo).toList());
        assertEquals(List.of(1, 2), repository.findById(alfa.getId()).orElseThrow().getPassos().stream()
                .map(PassoManual::getOrdem).toList());
    }

    /**
     * @apiNote Trocar a ordem dos relacionados não pode esbarrar na constraint única de
     *          (manual, relacionado) no meio da atualização das linhas.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void inverterAOrdemDosRelacionadosNaoViolaAConstraint() {
        alfa.getRelacionados().addAll(List.of(beta, gama));
        em.flush();

        alfa.getRelacionados().clear();
        alfa.getRelacionados().addAll(List.of(gama, beta));
        em.flush();
        verificarConstraintsAgora();
        em.clear();

        assertEquals(List.of("zzt-gama", "zzt-beta"), repository.findById(alfa.getId()).orElseThrow()
                .getRelacionados().stream().map(Manual::getTitulo).toList());
    }

    // ---------------------------------------------------------------- busca

    @Test
    void buscaPeloTitulo() {
        assertEquals(List.of("zzt-beta"), titulos(repository.buscar("%zzt-bet%", null, pagina())));
    }

    @Test
    void buscaPeloResumo() {
        assertEquals(List.of("zzt-alfa"), titulos(repository.buscar("%xilofone%", null, pagina())));
    }

    @Test
    void buscaPeloQuandoSeAplica() {
        assertEquals(List.of("zzt-gama"), titulos(repository.buscar("%bandolim%", null, pagina())));
    }

    @Test
    void buscaPeloAvisoDeAtualizacao() {
        assertEquals(List.of("zzt-beta"), titulos(repository.buscar("%clarinete%", null, pagina())));
    }

    @Test
    void buscaPelosPassos() {
        assertEquals(List.of("zzt-alfa"), titulos(repository.buscar("%trombone%", null, pagina())));
    }

    @Test
    void buscaPeloNomeDoLocal() {
        assertEquals(List.of("zzt-alfa"), titulos(repository.buscar("%saxofone%", null, pagina())));
    }

    @Test
    void buscaPelaAtencao() {
        assertEquals(List.of("zzt-alfa"), titulos(repository.buscar("%violino%", null, pagina())));
    }

    @Test
    void buscaPeloPrazoERegras() {
        assertEquals(List.of("zzt-beta"), titulos(repository.buscar("%fagote%", null, pagina())));
    }

    @Test
    void buscaPelosDetalhes() {
        assertEquals(List.of("zzt-gama"), titulos(repository.buscar("%tuba%", null, pagina())));
    }

    @Test
    void buscaPeloAntesDeComecar() {
        assertEquals(List.of("zzt-gama"), titulos(repository.buscar("%harpa%", null, pagina())));
    }

    /**
     * @apiNote A comparação é sem diferença de maiúsculas, inclusive em letra acentuada.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void buscaIgnoraMaiusculasInclusiveAcentuadas() {
        assertEquals(List.of("zzt-beta"), titulos(repository.buscar("%oboé maiusculo%", null, pagina())));
    }

    /**
     * @apiNote Um manual que casa em vários blocos continua aparecendo uma vez só.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void manualQueCasaEmVariosBlocosApareceUmaVez() {
        alfa.getDetalhes().add("zzt-repetido");
        alfa.getEntendaMelhor().add("zzt-repetido");
        alfa.getPassos().add(new PassoManual(3, "zzt-repetido"));
        em.flush();

        Page<Manual> pagina = repository.buscar("%zzt-repetido%", null, pagina());

        assertEquals(List.of("zzt-alfa"), titulos(pagina));
        assertEquals(1, pagina.getTotalElements());
    }

    /**
     * @apiNote O filtro de categoria sozinho lista a categoria inteira, na ordem da sequência.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void filtraPorCategoriaNaOrdemDaSequencia() {
        assertEquals(List.of("zzt-alfa", "zzt-beta"),
                titulos(repository.buscar("%zzt-%", CategoriaManual.MATRICULA_VIDA_ACADEMICA.name(), pagina())));
    }

    /**
     * @apiNote Termo e categoria combinam com E, não com OU.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void termoECategoriaCombinam() {
        assertTrue(repository.buscar("%tuba%", CategoriaManual.MATRICULA_VIDA_ACADEMICA.name(), pagina()).isEmpty());
    }

    /**
     * @apiNote Termo que não existe em lugar nenhum devolve página vazia com total zero.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void termoInexistenteDevolvePaginaVazia() {
        Page<Manual> pagina = repository.buscar("%zzt-nao-existe%", null, pagina());

        assertTrue(pagina.isEmpty());
        assertEquals(0, pagina.getTotalElements());
    }

    /**
     * @apiNote Sem filtros, todos os manuais vêm, e a contagem bate com o total do banco.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void semFiltrosTrazTodos() {
        assertEquals(repository.count(), repository.buscar(null, null, PageRequest.of(0, 1000)).getTotalElements());
    }

    /**
     * @apiNote A countQuery conta o total filtrado, e não só o tamanho da página.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void paginacaoContaOTotalFiltrado() {
        Page<Manual> primeira = repository.buscar("%zzt-%", null, PageRequest.of(0, 2));
        Page<Manual> segunda = repository.buscar("%zzt-%", null, PageRequest.of(1, 2));

        assertEquals(List.of("zzt-alfa", "zzt-beta"), titulos(primeira));
        assertEquals(List.of("zzt-gama"), titulos(segunda));
        assertEquals(3, primeira.getTotalElements());
        assertEquals(2, primeira.getTotalPages());
    }

    // ---------------------------------------------------------------- ordem e contagens

    @Test
    void maiorOrdemEADoUltimo() {
        assertEquals(base + 3, repository.maiorOrdem());
    }

    @Test
    void anteriorEProximoSaoOsVizinhosNaSequencia() {
        assertEquals("zzt-alfa", repository.findFirstByOrdemLessThanOrderByOrdemDesc(beta.getOrdem())
                .orElseThrow().getTitulo());
        assertEquals("zzt-gama", repository.findFirstByOrdemGreaterThanOrderByOrdemAsc(beta.getOrdem())
                .orElseThrow().getTitulo());
        assertTrue(repository.findFirstByOrdemGreaterThanOrderByOrdemAsc(gama.getOrdem()).isEmpty());
    }

    @Test
    void contaPorCategoria() {
        long antes = repository.countByCategoria(CategoriaManual.COMECANDO_UNISALES);
        em.persist(manual("zzt-delta", base + 4, CategoriaManual.COMECANDO_UNISALES));
        em.flush();

        assertEquals(antes + 1, repository.countByCategoria(CategoriaManual.COMECANDO_UNISALES));
    }

    /**
     * @apiNote Abrir espaço desloca várias linhas num UPDATE só. No meio dele duas linhas têm a
     *          mesma ordem, o que só funciona porque a constraint é deferida; o SET CONSTRAINTS
     *          força a checagem que normalmente só aconteceria no commit.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void deslocarOrdemAbreEspacoSemViolarAConstraint() {
        assertEquals(3, repository.deslocarOrdem(base + 1, base + 3, 1));
        verificarConstraintsAgora();
        em.clear();

        assertEquals(base + 2, repository.findById(alfa.getId()).orElseThrow().getOrdem());
        assertEquals(base + 4, repository.findById(gama.getId()).orElseThrow().getOrdem());
    }

    /**
     * @apiNote Fechar espaço desloca para trás; só os manuais do intervalo se movem.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void deslocarOrdemFechaEspacoSoNoIntervalo() {
        assertEquals(2, repository.deslocarOrdem(base + 2, base + 3, 1));
        assertEquals(2, repository.deslocarOrdem(base + 3, base + 4, -1));
        verificarConstraintsAgora();
        em.clear();

        assertEquals(base + 1, repository.findById(alfa.getId()).orElseThrow().getOrdem());
        assertEquals(base + 2, repository.findById(beta.getId()).orElseThrow().getOrdem());
    }

    /**
     * @apiNote Deferida não quer dizer desligada: duas ordens iguais ainda são barradas no commit.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void ordemRepetidaAindaEBarradaNoCommit() {
        em.persist(manual("zzt-duplicado", base + 1, CategoriaManual.MATRICULA_VIDA_ACADEMICA));
        em.flush();

        assertThrows(Exception.class, this::verificarConstraintsAgora);
    }

    /**
     * @apiNote Acha quem lista o manual em "Você também pode precisar".
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void encontraQuemRelacionaOManual() {
        alfa.getRelacionados().add(gama);
        beta.getRelacionados().add(gama);
        em.flush();

        assertEquals(List.of("zzt-alfa", "zzt-beta"), repository.findQueRelacionam(gama.getId()).stream()
                .map(Manual::getTitulo).sorted().toList());
        assertTrue(repository.findQueRelacionam(alfa.getId()).isEmpty());
    }

    @Test
    void verificaTituloIgnorandoMaiusculas() {
        assertTrue(repository.existsByTituloIgnoreCase("ZZT-ALFA"));
        assertFalse(repository.existsByTituloIgnoreCaseAndIdNot("ZZT-ALFA", alfa.getId()));
        assertTrue(repository.existsByTituloIgnoreCaseAndIdNot("ZZT-ALFA", beta.getId()));
    }

    private void verificarConstraintsAgora() {
        em.getEntityManager().createNativeQuery("SET CONSTRAINTS ALL IMMEDIATE").executeUpdate();
    }

    private Manual manual(String titulo, int ordem, CategoriaManual categoria) {
        Manual manual = new Manual();
        manual.setTitulo(titulo);
        manual.setOrdem(ordem);
        manual.setCategoria(categoria);
        manual.setTempoLeitura(1);
        manual.setResumo("resumo de " + titulo);
        manual.setOQueSaber("saber de " + titulo);
        return manual;
    }

    private PageRequest pagina() {
        return PageRequest.of(0, 20);
    }

    private List<String> titulos(Page<Manual> pagina) {
        return pagina.getContent().stream().map(Manual::getTitulo).toList();
    }
}
