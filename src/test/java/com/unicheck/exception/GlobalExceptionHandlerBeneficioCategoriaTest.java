package com.unicheck.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.hibernate.query.sqm.UnknownPathException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @apiNote Tradução de exceção para status HTTP nos caminhos usados por benefício e categoria.
 *
 *          O ponto delicado é a DataIntegrityViolationException, que chega por dois caminhos com
 *          tratamentos opostos: lançada pela Service, a mensagem vai inteira ao cliente; vinda do
 *          driver, é trocada por texto genérico para não expor detalhe de constraint.
 * @author Mateus Alves Costa
 * @since 19.09.2026
 */
class GlobalExceptionHandlerBeneficioCategoriaTest {
    private GlobalExceptionHandler handler;
    private HttpServletRequest request;

    /**
     * @apiNote Instancia o handler e a requisição usados por todos os testes.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @BeforeEach
    void preparar() {
        handler = new GlobalExceptionHandler();
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/beneficios");
    }

    /**
     * @apiNote EntityNotFoundException vira 404 com a mensagem preservada e o caminho em instance.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Test
    void entityNotFoundVira404PreservandoAMensagem() {
        ResponseEntity<ProblemDetail> resposta = handler.entityNotFound(
                new EntityNotFoundException("Benefício não encontrado com id: 42"), request);

        assertEquals(HttpStatus.NOT_FOUND, resposta.getStatusCode());
        assertEquals("Benefício não encontrado com id: 42", resposta.getBody().getDetail());
        assertEquals("/beneficios", resposta.getBody().getInstance().toString());
    }

    /**
     * @apiNote Sem causa, a exceção veio da Service e o texto já é voltado ao cliente.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Test
    void conflitoLancadoPelaServiceMantemAMensagemEspecifica() {
        ResponseEntity<ProblemDetail> resposta = handler.conflict(
                new ResourceConflictException("Já existe um benefício com o nome 'Bolsa'."), request);

        assertEquals(HttpStatus.CONFLICT, resposta.getStatusCode());
        assertEquals("Já existe um benefício com o nome 'Bolsa'.", resposta.getBody().getDetail());
    }

    /**
     * @apiNote Com causa, a exceção veio do driver: nome de tabela e de constraint não podem ir
     *          na resposta.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Test
    void conflitoVindoDoBancoNaoVazaDetalheTecnicoDaConstraint() {
        DataIntegrityViolationException doBanco = new DataIntegrityViolationException(
                "could not execute statement",
                new RuntimeException("ERROR: update or delete on table \"tb_categoria\" violates foreign key "
                        + "constraint \"tb_beneficio_categoria_categoria_id_fkey\""));

        ResponseEntity<ProblemDetail> resposta = handler.violacaoDeIntegridade(doBanco, request);

        assertEquals(HttpStatus.CONFLICT, resposta.getStatusCode());
        assertEquals("A operacao conflita com dados ja existentes ou ainda referenciados.", resposta.getBody().getDetail());
        assertFalse(resposta.getBody().getDetail().contains("tb_categoria"));
        assertFalse(resposta.getBody().getDetail().contains("RA"));
        assertFalse(resposta.getBody().getDetail().contains("foreign key"));
    }

    /**
     * @apiNote A exceção própria segue atendida, porque usuario e checklist ainda dependem dela.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Test
    void resourceConflictContinuaFuncionandoParaOsDominiosQueAindaAUsam() {
        ResponseEntity<ProblemDetail> resposta = handler.conflict(
                new ResourceConflictException("RA ja cadastrado."), request);

        assertEquals(HttpStatus.CONFLICT, resposta.getStatusCode());
        assertEquals("RA ja cadastrado.", resposta.getBody().getDetail());
    }

    /**
     * @apiNote Sort apontando para propriedade que não existe é erro do cliente, com o nome na
     *          mensagem e o parâmetro identificado.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Test
    void sortComPropriedadeInexistenteVira400ENao500() {
        PropertyReferenceException erro = mock(PropertyReferenceException.class);
        when(erro.getPropertyName()).thenReturn("string");

        ResponseEntity<ProblemDetail> resposta = handler.sortInvalido(erro, request);

        assertEquals(HttpStatus.BAD_REQUEST, resposta.getStatusCode());
        assertEquals("Nao e possivel ordenar por 'string'.", resposta.getBody().getDetail());
        assertEquals("sort", resposta.getBody().getProperties().get("parameter"));
    }

    /**
     * @apiNote No caminho do @Query o Hibernate embrulha a UnknownPathException, então olhar só a
     *          causa direta deixaria passar como 500.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Test
    void sortInvalidoEmQueryVira400MesmoComACausaAninhada() {
        InvalidDataAccessApiUsageException erro = new InvalidDataAccessApiUsageException(
                "Could not resolve attribute",
                new IllegalArgumentException(new UnknownPathException("Could not resolve attribute 'string'")));

        ResponseEntity<ProblemDetail> resposta = handler.sortInvalidoEmQuery(erro, request);

        assertEquals(HttpStatus.BAD_REQUEST, resposta.getStatusCode());
        assertEquals("sort", resposta.getBody().getProperties().get("parameter"));
    }

    /**
     * @apiNote Uso indevido de API que não seja sort é bug de código, e não erro do cliente.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Test
    void invalidDataAccessSemCaminhoDesconhecidoContinua500() {
        InvalidDataAccessApiUsageException erro = new InvalidDataAccessApiUsageException(
                "Parameter value did not match expected type", new IllegalArgumentException("outro problema"));

        ResponseEntity<ProblemDetail> resposta = handler.sortInvalidoEmQuery(erro, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resposta.getStatusCode());
        assertEquals("Ocorreu um erro inesperado. Tente novamente mais tarde.", resposta.getBody().getDetail());
    }

    /**
     * @apiNote Todo corpo de erro mantém o contrato: instance, timestamp e title preenchidos.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Test
    void todaRespostaDeErroCarregaInstanceETimestamp() {
        when(request.getRequestURI()).thenReturn("/categorias/9");

        ProblemDetail problema = handler.entityNotFound(
                new EntityNotFoundException("Categoria não encontrada com id: 9"), request).getBody();

        assertEquals("/categorias/9", problema.getInstance().toString());
        assertNotNull(problema.getProperties().get("timestamp"));
        assertEquals("Recurso nao encontrado", problema.getTitle());
    }
}
