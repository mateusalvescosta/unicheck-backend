package com.unicheck.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Tratador global de exceções da aplicação.
 *
 * Centraliza o tratamento de erros lançados pelos controllers,
 * retornando respostas padronizadas no formato ProblemDetail
 * (RFC 7807), utilizado pelo Spring Boot 3.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Trata exceções lançadas quando um recurso não é encontrado.
     *
     * Exemplo:
     * - Buscar um aluno utilizando um ID inexistente.
     *
     * Retorna HTTP 404 (Not Found).
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleResourceNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request) {

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);

        problem.setTitle("Recurso não encontrado");
        problem.setDetail(ex.getMessage());
        problem.setInstance(URI.create(request.getRequestURI()));

        problem.setProperty("timestamp", OffsetDateTime.now());
        problem.setProperty("code", "UC-404");

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(problem);
    }

    /**
     * Trata erros de validação dos dados enviados na requisição.
     *
     * Ocorre quando uma classe anotada com @Valid possui
     * algum campo inválido.
     *
     * Retorna HTTP 422 (Unprocessable Entity).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_ENTITY);

        problem.setTitle("Dados inválidos");
        problem.setDetail("Um ou mais campos da requisição são inválidos.");
        problem.setInstance(URI.create(request.getRequestURI()));

        problem.setProperty("timestamp", OffsetDateTime.now());
        problem.setProperty("code", "UC-422");

        List<Map<String, String>> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> Map.of(
                        "field", error.getField(),
                        "message", error.getDefaultMessage()
                ))
                .toList();

        problem.setProperty("errors", errors);

        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(problem);
    }

    /**
     * Trata violações de integridade no banco de dados.
     *
     * Exemplos:
     * - Email já cadastrado.
     * - Violação de chave única.
     * - Exclusão de registro referenciado por outra entidade.
     *
     * Retorna HTTP 400 (Bad Request).
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrity(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);

        problem.setTitle("Violação de integridade");
        problem.setDetail(ex.getMostSpecificCause().getMessage());
        problem.setInstance(URI.create(request.getRequestURI()));

        problem.setProperty("timestamp", OffsetDateTime.now());
        problem.setProperty("code", "UC-400");

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(problem);
    }

    /**
     * Trata qualquer exceção não tratada especificamente.
     *
     * Funciona como uma "rede de segurança", evitando que
     * detalhes internos da aplicação sejam enviados ao cliente.
     *
     * Retorna HTTP 500 (Internal Server Error).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGeneric(
            Exception ex,
            HttpServletRequest request) {

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);

        problem.setTitle("Erro interno");
        problem.setDetail("Ocorreu um erro inesperado. Tente novamente mais tarde.");
        problem.setInstance(URI.create(request.getRequestURI()));

        problem.setProperty("timestamp", OffsetDateTime.now());
        problem.setProperty("code", "UC-500");

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(problem);
    }
}