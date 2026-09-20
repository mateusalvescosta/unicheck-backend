package com.unicheck.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.core.PropertyReferenceException;
import org.hibernate.query.sqm.UnknownPathException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/** Centraliza erros como ProblemDetail para manter o contrato RFC 7807 da API. */
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> notFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, "Recurso nao encontrado", ex.getMessage(), request);
    }

    /**
     * Mesmo 404 da ResourceNotFoundException, para os dominios que preferem a
     * excecao ja definida pela JPA em vez de uma propria.
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ProblemDetail> entityNotFound(EntityNotFoundException ex, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, "Recurso nao encontrado", ex.getMessage(), request);
    }

    /** Validacao de entrada e erro do cliente, por isso retorna 400 e os campos afetados. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> validation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        ProblemDetail problem = base(HttpStatus.BAD_REQUEST, "Dados invalidos", "Um ou mais campos da requisicao sao invalidos.", request);
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of("field", error.getField(), "message", error.getDefaultMessage())).toList();
        problem.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problem);
    }

    /** Filtro com valor fora do enum, como ?tipo=GRATIS, e erro do cliente e nao 500. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> typeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        ProblemDetail problem = base(HttpStatus.BAD_REQUEST, "Parametro invalido",
                "O valor enviado nao e aceito por este parametro.", request);
        problem.setProperty("parameter", ex.getName());
        return ResponseEntity.badRequest().body(problem);
    }

    /** Sort apontando para propriedade que nao existe, como ?sort=string, e erro do cliente e nao 500. */
    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ProblemDetail> sortInvalido(PropertyReferenceException ex, HttpServletRequest request) {
        ProblemDetail problem = base(HttpStatus.BAD_REQUEST, "Parametro invalido",
                "Nao e possivel ordenar por '" + ex.getPropertyName() + "'.", request);
        problem.setProperty("parameter", "sort");
        return ResponseEntity.badRequest().body(problem);
    }

    /** O mesmo sort invalido chega embrulhado quando a consulta e um @Query, e nao como PropertyReferenceException. */
    @ExceptionHandler(InvalidDataAccessApiUsageException.class)
    public ResponseEntity<ProblemDetail> sortInvalidoEmQuery(InvalidDataAccessApiUsageException ex, HttpServletRequest request) {
        // O Hibernate ainda embrulha a UnknownPathException em IllegalArgumentException antes de o Spring converter,
        // entao nao basta olhar a causa direta. O limite de profundidade encerra o laco mesmo que a
        // cadeia de causas seja ciclica, o que comparar apenas a causa imediata nao garante.
        boolean propriedadeInexistente = false;
        Throwable causa = ex;
        for (int nivel = 0; causa != null && nivel < 10; nivel++, causa = causa.getCause()) {
            if (causa instanceof UnknownPathException) {
                propriedadeInexistente = true;
                break;
            }
        }
        if (!propriedadeInexistente) {
            return response(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno",
                    "Ocorreu um erro inesperado. Tente novamente mais tarde.", request);
        }
        ProblemDetail problem = base(HttpStatus.BAD_REQUEST, "Parametro invalido",
                "Nao e possivel ordenar pela propriedade informada.", request);
        problem.setProperty("parameter", "sort");
        return ResponseEntity.badRequest().body(problem);
    }

    /** Nunca diferencia RA inexistente de senha errada para evitar enumeracao de usuarios. */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ProblemDetail> badCredentials(BadCredentialsException ex, HttpServletRequest request) {
        return response(HttpStatus.UNAUTHORIZED, "Falha na autenticacao", "RA ou senha incorretos", request);
    }

    /** A senha atual invalida nao altera a sessao, mas a operacao solicitada nao e permitida. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> accessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return response(HttpStatus.FORBIDDEN, "Acesso negado", "Nao foi possivel concluir esta operacao.", request);
    }

    @ExceptionHandler({ResourceConflictException.class, DataIntegrityViolationException.class})
    public ResponseEntity<ProblemDetail> conflict(Exception ex, HttpServletRequest request) {
        // A constraint do banco ainda pode vencer uma verificacao concorrente; ambos os caminhos devem responder 409.
        //
        // A DataIntegrityViolationException chega por dois caminhos: lancada pela Service, ja com a
        // mensagem pronta e sem causa, ou vinda do driver, embrulhando o erro tecnico da constraint.
        // So o segundo caso precisa de texto generico, porque a mensagem original nao serve ao cliente.
        boolean mensagemPropria = ex instanceof ResourceConflictException || ex.getCause() == null;
        String detail = mensagemPropria ? ex.getMessage() : "RA ou email ja cadastrado, ou registro ainda referenciado.";
        return response(HttpStatus.CONFLICT, "Conflito de dados", detail, request);
    }

    private ResponseEntity<ProblemDetail> response(HttpStatus status, String title, String detail, HttpServletRequest request) {
        return ResponseEntity.status(status).body(base(status, title, detail, request));
    }
    private ProblemDetail base(HttpStatus status, String title, String detail, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title); problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", OffsetDateTime.now());
        return problem;
    }
}