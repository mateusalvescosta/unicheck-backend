package com.unicheck.beneficio.repository;

import com.unicheck.beneficio.domain.Beneficio;
import com.unicheck.beneficio.enumeration.TipoBeneficio;
import com.unicheck.categoria.domain.Categoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @apiNote Exercita o SQL nativo de BeneficioRepository.buscar contra o Postgres de verdade.
 *
 *          O teste unitário da Service mocka o repository, então os EXISTS, os CAST, o ORDER BY e a
 *          countQuery não são cobertos por ele. Aqui eles rodam.
 *
 *          Cada teste roda em transação revertida, e todo fixture carrega o prefixo "zzt-" para que
 *          as buscas nunca alcancem dados que já estejam no banco. A classe é ignorada quando não há
 *          Postgres na porta esperada, de modo que o build continue verde em quem não tem o banco de
 *          desenvolvimento no ar.
 * @author Mateus Alves Costa
 * @since 20.09.2026
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnabledIf("bancoDisponivel")
class BeneficioRepositoryBuscaTest {
    private static final String PREFIXO = "%zzt-%";

    @Autowired BeneficioRepository repository;
    @Autowired TestEntityManager em;

    private Categoria ativa;
    private Categoria desativada;

    static boolean bancoDisponivel() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", 5432), 500);
            return true;
        } catch (Exception naoDisponivel) {
            return false;
        }
    }

    /**
     * @apiNote Semeia três benefícios que cobrem, entre si, todos os campos alcançados pela busca.
     * @author Mateus Alves Costa
     * @since 20.09.2026
     */
    @BeforeEach
    void semear() {
        ativa = categoria("zzt-cat-ativa", true);
        desativada = categoria("zzt-cat-inativa", false);

        beneficio("zzt-alfa", "descricao xilofone", "chamada alfa", "publico alfa",
                TipoBeneficio.GRATUITO, ativa, "zzt-palavra-unica");
        beneficio("zzt-beta", "descricao beta", "chamada trombone", "publico beta",
                TipoBeneficio.DESCONTO, desativada, null);
        beneficio("zzt-gama", "descricao gama", "chamada gama", "publico saxofone",
                TipoBeneficio.GRATUITO, null, null);
        em.flush();
    }

    /**
     * @apiNote O termo alcança o nome do benefício.
     * @author Mateus Alves Costa
     * @since 20.09.2026
     */
    @Test
    void casaPeloNome() {
        assertEquals(List.of("zzt-alfa"), nomesDe(repository.buscar("%zzt-alf%", null, null, pagina())));
    }

    /**
     * @apiNote O termo alcança a descrição.
     * @author Mateus Alves Costa
     * @since 20.09.2026
     */
    @Test
    void casaPelaDescricao() {
        assertEquals(List.of("zzt-alfa"), nomesDe(repository.buscar("%xilofone%", null, null, pagina())));
    }

    /**
     * @apiNote O termo alcança a chamada.
     * @author Mateus Alves Costa
     * @since 20.09.2026
     */
    @Test
    void casaPelaChamada() {
        assertEquals(List.of("zzt-beta"), nomesDe(repository.buscar("%trombone%", null, null, pagina())));
    }

    /**
     * @apiNote O termo alcança o público elegível.
     * @author Mateus Alves Costa
     * @since 20.09.2026
     */
    @Test
    void casaPeloPublicoElegivel() {
        assertEquals(List.of("zzt-gama"), nomesDe(repository.buscar("%saxofone%", null, null, pagina())));
    }

    /**
     * @apiNote O termo alcança o nome da categoria. O benefício não tem esse texto em campo próprio,
     *          então só o EXISTS de categoria o encontra.
     * @author Mateus Alves Costa
     * @since 20.09.2026
     */
    @Test
    void casaPeloNomeDaCategoria() {
        assertEquals(List.of("zzt-alfa"), nomesDe(repository.buscar("%zzt-cat-ativa%", null, null, pagina())));
    }

    /**
     * @apiNote O termo alcança a palavra-chave, pelo EXISTS da coleção.
     * @author Mateus Alves Costa
     * @since 20.09.2026
     */
    @Test
    void casaPelaPalavraChave() {
        assertEquals(List.of("zzt-alfa"), nomesDe(repository.buscar("%palavra-unica%", null, null, pagina())));
    }

    /**
     * @apiNote O padrão chega da Service já em minúsculas; o LOWER do SQL cuida do lado da coluna.
     * @author Mateus Alves Costa
     * @since 20.09.2026
     */
    @Test
    void buscaIgnoraMaiusculasEMinusculas() {
        assertEquals(List.of("zzt-alfa"), nomesDe(repository.buscar("%xilofone%", null, null, pagina())));
        assertEquals(List.of("zzt-alfa"), nomesDe(repository.buscar("%xilofone%", null, null, pagina())));
    }

    /**
     * @apiNote Termo sem correspondência devolve página vazia e total zero.
     * @author Mateus Alves Costa
     * @since 20.09.2026
     */
    @Test
    void termoQueNaoCasaDevolveVazio() {
        Page<Beneficio> retorno = repository.buscar("%zzt-inexistente%", null, null, pagina());

        assertTrue(retorno.isEmpty());
        assertEquals(0, retorno.getTotalElements());
    }

    /**
     * @apiNote O filtro de tipo funciona sozinho, sem termo de texto.
     * @author Mateus Alves Costa
     * @since 20.09.2026
     */
    @Test
    void filtraPorTipoSemTermo() {
        Page<Beneficio> retorno = repository.buscar(PREFIXO, TipoBeneficio.DESCONTO.name(), null, pagina());

        assertEquals(List.of("zzt-beta"), nomesDe(retorno));
    }

    /**
     * @apiNote O filtro de categoria devolve apenas os benefícios vinculados a ela.
     * @author Mateus Alves Costa
     * @since 20.09.2026
     */
    @Test
    void filtraPorCategoria() {
        Page<Beneficio> retorno = repository.buscar(PREFIXO, null, ativa.getId(), pagina());

        assertEquals(List.of("zzt-alfa"), nomesDe(retorno));
    }

    /**
     * @apiNote Desativar categoria só impede novos vínculos; a busca não pode esconder o que já existe.
     * @author Mateus Alves Costa
     * @since 20.09.2026
     */
    @Test
    void beneficioDeCategoriaDesativadaContinuaAparecendo() {
        assertEquals(List.of("zzt-beta"), nomesDe(repository.buscar(PREFIXO, null, desativada.getId(), pagina())));
        assertTrue(nomesDe(repository.buscar(PREFIXO, null, null, pagina())).contains("zzt-beta"));
    }

    /**
     * @apiNote Tipo e categoria se acumulam em vez de competir.
     * @author Mateus Alves Costa
     * @since 20.09.2026
     */
    @Test
    void filtrosCombinadosSeAcumulam() {
        assertTrue(repository.buscar(PREFIXO, TipoBeneficio.GRATUITO.name(), desativada.getId(), pagina()).isEmpty());
        assertEquals(List.of("zzt-beta"),
                nomesDe(repository.buscar(PREFIXO, TipoBeneficio.DESCONTO.name(), desativada.getId(), pagina())));
    }

    /**
     * @apiNote O ORDER BY vive no SQL, então a ordem sai correta com o Pageable sem ordenação.
     * @author Mateus Alves Costa
     * @since 20.09.2026
     */
    @Test
    void ordenaPorNomeMesmoSemSortNoPageable() {
        assertEquals(List.of("zzt-alfa", "zzt-beta", "zzt-gama"),
                nomesDe(repository.buscar(PREFIXO, null, null, pagina())));
    }

    /**
     * @apiNote O caso que motivou trocar JOIN por EXISTS: com JOIN este benefício viria repetido e a
     *          contagem da página não fecharia com o conteúdo.
     * @author Mateus Alves Costa
     * @since 20.09.2026
     */
    @Test
    void varioasCategoriasEPalavrasCasandoAindaRendemUmaLinhaSo() {
        Beneficio multiplo = beneficio("zzt-delta", "descricao zzt-repetido", "chamada zzt-repetido",
                "publico zzt-repetido", TipoBeneficio.GRATUITO, null, null);
        multiplo.getCategorias().add(categoria("zzt-repetido-um", true));
        multiplo.getCategorias().add(categoria("zzt-repetido-dois", true));
        multiplo.getPalavrasChave().add("zzt-repetido-a");
        multiplo.getPalavrasChave().add("zzt-repetido-b");
        em.flush();

        Page<Beneficio> retorno = repository.buscar("%zzt-repetido%", null, null, pagina());

        assertEquals(1, retorno.getNumberOfElements());
        assertEquals(1, retorno.getTotalElements());
        assertEquals("zzt-delta", retorno.getContent().get(0).getNome());
    }

    /**
     * @apiNote A countQuery é independente da query de conteúdo; divergir aqui quebraria a navegação.
     * @author Mateus Alves Costa
     * @since 20.09.2026
     */
    @Test
    void paginacaoDivideOResultadoEMantemATotalizacao() {
        Page<Beneficio> primeira = repository.buscar(PREFIXO, null, null, PageRequest.of(0, 2));
        Page<Beneficio> segunda = repository.buscar(PREFIXO, null, null, PageRequest.of(1, 2));

        assertEquals(List.of("zzt-alfa", "zzt-beta"), nomesDe(primeira));
        assertEquals(List.of("zzt-gama"), nomesDe(segunda));
        assertEquals(3, primeira.getTotalElements());
        assertEquals(3, segunda.getTotalElements());
        assertEquals(2, primeira.getTotalPages());
    }

    /**
     * @apiNote Página além do fim vem vazia, mas ainda informa o total.
     * @author Mateus Alves Costa
     * @since 20.09.2026
     */
    @Test
    void paginaAlemDoFimVemVaziaSemPerderOTotal() {
        Page<Beneficio> retorno = repository.buscar(PREFIXO, null, null, PageRequest.of(9, 2));

        assertTrue(retorno.getContent().isEmpty());
        assertEquals(3, retorno.getTotalElements());
    }

    /**
     * @apiNote O CAST do parâmetro aguenta um id que não casa com nenhuma linha.
     * @author Mateus Alves Costa
     * @since 20.09.2026
     */
    @Test
    void categoriaInexistenteNaoDerrubaAConsulta() {
        assertTrue(repository.buscar(PREFIXO, null, -1L, pagina()).isEmpty());
    }

    /**
     * @apiNote Monta a paginação padrão dos testes, sem ordenação.
     * @return --> Retorna a paginação montada.
     * @author Mateus Alves Costa
     * @since 20.09.2026
     */
    private PageRequest pagina() {
        return PageRequest.of(0, 20);
    }

    /**
     * @apiNote Extrai os nomes dos benefícios de uma página, na ordem em que vieram.
     * @param pagina --> Página devolvida pela busca.
     * @return --> Retorna os nomes em ordem.
     * @author Mateus Alves Costa
     * @since 20.09.2026
     */
    private List<String> nomesDe(Page<Beneficio> pagina) {
        return pagina.getContent().stream().map(Beneficio::getNome).toList();
    }

    /**
     * @apiNote Persiste uma Categoria de teste.
     * @param nome --> Nome da categoria.
     * @param ativo --> Estado desejado.
     * @return --> Retorna a categoria já persistida.
     * @author Mateus Alves Costa
     * @since 20.09.2026
     */
    private Categoria categoria(String nome, boolean ativo) {
        Categoria categoria = new Categoria();
        categoria.setNome(nome);
        categoria.setAtivo(ativo);
        return em.persist(categoria);
    }

    /**
     * @apiNote Persiste um Beneficio de teste com os campos obrigatórios preenchidos.
     * @param nome --> Nome do benefício.
     * @param descricao --> Descrição do benefício.
     * @param chamada --> Chamada do card.
     * @param publicoElegivel --> Público elegível.
     * @param tipo --> Tipo do benefício.
     * @param categoria --> Categoria a vincular, ou null para nenhuma.
     * @param palavraChave --> Palavra-chave a incluir, ou null para nenhuma.
     * @return --> Retorna o benefício já persistido.
     * @author Mateus Alves Costa
     * @since 20.09.2026
     */
    private Beneficio beneficio(String nome, String descricao, String chamada, String publicoElegivel,
            TipoBeneficio tipo, Categoria categoria, String palavraChave) {
        Beneficio beneficio = new Beneficio();
        beneficio.setNome(nome);
        beneficio.setDescricao(descricao);
        beneficio.setChamada(chamada);
        beneficio.setPublicoElegivel(publicoElegivel);
        beneficio.setUrl("https://zzt.local");
        beneficio.setTipo(tipo);
        if (categoria != null) {
            beneficio.getCategorias().add(categoria);
        }
        if (palavraChave != null) {
            beneficio.getPalavrasChave().add(palavraChave);
        }
        return em.persist(beneficio);
    }
}
