package com.unicheck.manual.dto;

import com.unicheck.manual.enumeration.CategoriaManual;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @apiNote Regras de Bean Validation dos DTOs de entrada do manual, exercitadas direto no
 *          Validator, sem Spring.
 *
 *          O ManualService confia nelas: não repete checagem de obrigatório, de tamanho nem de
 *          item nulo nas listas. Se alguma anotação sumir, o erro deixa de ser 400 e passa a ser
 *          NullPointerException ou estouro de coluna no banco, por isso cada regra tem teste.
 * @author Mateus Alves Costa
 * @since 27.09.2026
 */
class ManualDTOValidationTest {
    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void criarValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void fecharValidator() {
        factory.close();
    }

    /**
     * @apiNote O manual mínimo, só com os campos presentes em todas as páginas, é válido.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void manualMinimoEValido() {
        assertTrue(validator.validate(new Manual().build()).isEmpty());
    }

    /**
     * @apiNote O manual com todos os blocos preenchidos também é válido.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void manualCompletoEValido() {
        Manual completo = new Manual();
        completo.ordem = 3;
        completo.linkTexto = "Abrir Portal Acadêmico";
        completo.linkUrl = "https://portal.exemplo.edu.br";
        completo.quandoSeAplica = "Para consultar registros acadêmicos.";
        completo.atencao = List.of("Não renovar caracteriza abandono.");
        completo.antesDeComecar = List.of("Ter seu RA.");
        completo.passos = List.of(new PassoManualDTO(1, "Acesse o Portal."));
        completo.ondeResolver = List.of(new LocalAtendimentoDTO("Multiatendimento", null));
        completo.prazoERegras = List.of("Siga o prazo do edital.");
        completo.resultadoEsperado = "Vínculo renovado.";
        completo.detalhes = List.of("O Portal registra a vida acadêmica.");
        completo.entendaMelhor = List.of("Conteúdos ficam no AVA.");
        completo.proximoPassoDescricao = "Veja a orientação do AVA.";
        completo.proximoPassoManualId = 2L;
        completo.avisoAtualizacao = "Prazos podem mudar.";
        completo.relacionadoIds = List.of(4L, 5L, 6L);

        assertTrue(validator.validate(completo.build()).isEmpty());
    }

    /**
     * @apiNote Os cinco campos obrigatórios são rejeitados quando ausentes.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void camposObrigatoriosAusentesSaoRejeitados() {
        Manual vazio = new Manual();
        vazio.categoria = null;
        vazio.tempoLeitura = null;
        vazio.titulo = null;
        vazio.resumo = null;
        vazio.oQueSaber = null;

        assertEquals(Set.of("categoria", "tempoLeitura", "titulo", "resumo", "oQueSaber"),
                campos(validator.validate(vazio.build())));
    }

    /**
     * @apiNote Texto só com espaços não conta como preenchido nos obrigatórios.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void textoObrigatorioEmBrancoERejeitado() {
        Manual emBranco = new Manual();
        emBranco.titulo = "   ";
        emBranco.resumo = "";
        emBranco.oQueSaber = " ";

        assertEquals(Set.of("titulo", "resumo", "oQueSaber"), campos(validator.validate(emBranco.build())));
    }

    /**
     * @apiNote Ordem e tempo de leitura precisam ser maiores que zero.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void ordemETempoDeLeituraNaoPositivosSaoRejeitados() {
        Manual invalido = new Manual();
        invalido.ordem = 0;
        invalido.tempoLeitura = -1;

        assertEquals(Set.of("ordem", "tempoLeitura"), campos(validator.validate(invalido.build())));
    }

    /**
     * @apiNote O limite de cada texto é o tamanho da coluna: no limite passa, um a mais não.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void textoNoLimiteDaColunaPassaEUmCaractereAMaisNao() {
        Manual noLimite = new Manual();
        noLimite.titulo = "a".repeat(150);
        noLimite.oQueSaber = "a".repeat(1000);
        assertTrue(validator.validate(noLimite.build()).isEmpty());

        Manual acima = new Manual();
        acima.titulo = "a".repeat(151);
        acima.resumo = "a".repeat(256);
        acima.oQueSaber = "a".repeat(1001);
        acima.quandoSeAplica = "a".repeat(501);
        acima.avisoAtualizacao = "a".repeat(256);
        assertEquals(Set.of("titulo", "resumo", "oQueSaber", "quandoSeAplica", "avisoAtualizacao"),
                campos(validator.validate(acima.build())));
    }

    /**
     * @apiNote Texto do link sem URL, e o contrário, é rejeitado.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void linkPelaMetadeERejeitado() {
        Manual soTexto = new Manual();
        soTexto.linkTexto = "Abrir Portal";
        assertEquals(Set.of("linkCompleto"), campos(validator.validate(soTexto.build())));

        Manual soUrl = new Manual();
        soUrl.linkUrl = "https://portal.exemplo.edu.br";
        soUrl.linkTexto = "  ";
        assertEquals(Set.of("linkCompleto"), campos(validator.validate(soUrl.build())));
    }

    /**
     * @apiNote "Ver orientação" sem o texto de "Seu próximo passo" é rejeitado; o texto sozinho,
     *          como na página de Contatos, é aceito.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void proximoPassoPrecisaDeTextoQuandoIndicaManual() {
        Manual semTexto = new Manual();
        semTexto.proximoPassoManualId = 2L;
        assertEquals(Set.of("proximoPassoCompleto"), campos(validator.validate(semTexto.build())));

        Manual soTexto = new Manual();
        soTexto.proximoPassoDescricao = "Acesse o site institucional.";
        assertTrue(validator.validate(soTexto.build()).isEmpty());
    }

    /**
     * @apiNote Três relacionados é o limite; quatro é rejeitado.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void maisDeTresRelacionadosERejeitado() {
        Manual quatro = new Manual();
        quatro.relacionadoIds = List.of(1L, 2L, 3L, 4L);

        assertEquals(Set.of("relacionadoIds"), campos(validator.validate(quatro.build())));
    }

    /**
     * @apiNote Item nulo ou em branco dentro das listas é rejeitado; a Service faria trim nele.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void itemNuloOuEmBrancoNasListasERejeitado() {
        Manual invalido = new Manual();
        invalido.atencao = List.of(" ");
        invalido.antesDeComecar = Collections.singletonList(null);
        invalido.entendaMelhor = List.of("a".repeat(501));
        invalido.relacionadoIds = Collections.singletonList(null);

        Set<String> campos = campos(validator.validate(invalido.build()));

        assertTrue(campos.stream().anyMatch(campo -> campo.startsWith("atencao")));
        assertTrue(campos.stream().anyMatch(campo -> campo.startsWith("antesDeComecar")));
        assertTrue(campos.stream().anyMatch(campo -> campo.startsWith("entendaMelhor")));
        assertTrue(campos.stream().anyMatch(campo -> campo.startsWith("relacionadoIds")));
    }

    /**
     * @apiNote Passo e local inválidos são validados em cascata, dentro da lista.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void passoELocalInvalidosSaoValidadosEmCascata() {
        Manual invalido = new Manual();
        invalido.passos = List.of(new PassoManualDTO(0, " "));
        invalido.ondeResolver = List.of(new LocalAtendimentoDTO("", "a".repeat(256)));

        assertEquals(Set.of("passos[0].ordem", "passos[0].descricao", "ondeResolver[0].nome",
                "ondeResolver[0].url"), campos(validator.validate(invalido.build())));
    }

    /**
     * @apiNote Passo nulo dentro da lista é rejeitado; a Service ordenaria a lista por ele.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void passoNuloNaListaERejeitado() {
        Manual invalido = new Manual();
        invalido.passos = Collections.singletonList(null);

        assertFalse(validator.validate(invalido.build()).isEmpty());
    }

    /**
     * @apiNote Oito atalhos é o limite da grade; nove é rejeitado.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void maisDeOitoAtalhosERejeitado() {
        List<AtalhoManualDTO> oito = new ArrayList<>();
        for(long i = 1; i <= 8; i++) oito.add(new AtalhoManualDTO(i, "Atalho " + i));
        assertTrue(validator.validate(new AtalhosManualRequestDTO(oito)).isEmpty());

        List<AtalhoManualDTO> nove = new ArrayList<>(oito);
        nove.add(new AtalhoManualDTO(9L, "Atalho 9"));
        assertEquals(Set.of("atalhos"), campos(validator.validate(new AtalhosManualRequestDTO(nove))));
    }

    /**
     * @apiNote Lista de atalhos vazia é válida (remove todos), mas ausente não.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void listaDeAtalhosVaziaEValidaMasAusenteNao() {
        assertTrue(validator.validate(new AtalhosManualRequestDTO(List.of())).isEmpty());
        assertEquals(Set.of("atalhos"), campos(validator.validate(new AtalhosManualRequestDTO(null))));
    }

    /**
     * @apiNote Atalho sem manual, sem nome ou com nome longo demais é rejeitado em cascata.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void atalhoInvalidoERejeitadoEmCascata() {
        AtalhosManualRequestDTO dto = new AtalhosManualRequestDTO(List.of(
                new AtalhoManualDTO(null, " "),
                new AtalhoManualDTO(2L, "a".repeat(61))));

        assertEquals(Set.of("atalhos[0].manualId", "atalhos[0].nome", "atalhos[1].nome"),
                campos(validator.validate(dto)));
    }

    /**
     * @apiNote Atalho nulo dentro da lista é rejeitado.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void atalhoNuloNaListaERejeitado() {
        assertFalse(validator.validate(new AtalhosManualRequestDTO(Collections.singletonList(null))).isEmpty());
    }

    private static <T> Set<String> campos(Set<ConstraintViolation<T>> violacoes) {
        return violacoes.stream().map(violacao -> violacao.getPropertyPath().toString())
                .map(caminho -> caminho.replaceAll("\\.<[^>]+>$", ""))
                .collect(Collectors.toSet());
    }

    /**
     * @apiNote Monta um ManualRequestDTO válido por padrão; cada teste troca só o que interessa,
     *          em vez de repetir os 21 argumentos do record.
     */
    private static class Manual {
        Integer ordem;
        CategoriaManual categoria = CategoriaManual.PORTAL_FERRAMENTAS_DIGITAIS;
        Integer tempoLeitura = 1;
        String titulo = "Portal Acadêmico";
        String resumo = "Conheça o canal.";
        String linkTexto;
        String linkUrl;
        String quandoSeAplica;
        String oQueSaber = "O Portal Acadêmico é o sistema.";
        List<String> atencao;
        List<String> antesDeComecar;
        List<PassoManualDTO> passos;
        List<LocalAtendimentoDTO> ondeResolver;
        List<String> prazoERegras;
        String resultadoEsperado;
        List<String> detalhes;
        List<String> entendaMelhor;
        String proximoPassoDescricao;
        Long proximoPassoManualId;
        String avisoAtualizacao;
        List<Long> relacionadoIds;

        ManualRequestDTO build() {
            return new ManualRequestDTO(ordem, categoria, tempoLeitura, titulo, resumo, linkTexto, linkUrl,
                    quandoSeAplica, oQueSaber, atencao, antesDeComecar, passos, ondeResolver, prazoERegras,
                    resultadoEsperado, detalhes, entendaMelhor, proximoPassoDescricao, proximoPassoManualId,
                    avisoAtualizacao, relacionadoIds);
        }
    }
}
