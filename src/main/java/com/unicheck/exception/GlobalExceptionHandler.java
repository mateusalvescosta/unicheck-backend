package com.unicheck.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
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
    @ExceptionHandler(EntityNotFoundException.class)
    public ProblemDetail handleEntityNotFound(EntityNotFoundException ex, HttpServletRequest request) {

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);

        problem.setProperty("timestamp", Instant.now());
        problem.setTitle("Recurso não encontrado.");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setStatus(404);

        return problem;
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
    public ProblemDetail handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpServletRequest request) {

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_ENTITY);

        problem.setProperty("timestamp", Instant.now());
        problem.setTitle("Um ou mais dados inválidos.");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setStatus(422);

        List<Map<String, String>> errors = ex.getBindingResult()
                .getFieldErrors().stream()
                .map(error -> Map.of(
                        "field", error.getField(),
                        "message", error.getDefaultMessage()
                )).toList();

        problem.setProperty("errors", errors);

        return problem;
    }

    /**
     * Trata violações de integridade no banco de dados que não foram
     * antecipadas pela camada de serviço (ex: corrida entre duas requisições
     * simultâneas, ou remoção de registro ainda referenciado por outro).
     *
     * A distinção entre duplicidade e registro em uso é feita pelo método
     * HTTP da requisição, nunca por parsing da mensagem do banco — assim
     * nenhum nome de tabela, constraint ou SQL chega ao cliente (RF-SEC-004).
     * O detalhe original vai só para o log, com um id de rastreio.
     *
     * Retorna HTTP 409 (Conflict).
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request) {

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        
        problem.setProperty("timestamp", Instant.now());
        problem.setTitle("Operação não permitida: violação de integridade dos dados.");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setStatus(409);

        return problem;
    }

}