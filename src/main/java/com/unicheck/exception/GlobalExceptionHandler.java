package com.unicheck.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

/**
 * Tratador de erros central da aplicação.
 *
 * Intercepta exceptions lançadas por QUALQUER controller do projeto e as
 * converte em respostas JSON padronizadas (ErrorResponse), com o status
 * HTTP correto. Sem essa classe, cada controller precisaria tratar erro
 * manualmente, e o Spring devolveria stack traces feios por padrão.
 *
 * Ao criar uma nova exception customizada no projeto (ex: validação de
 * regra de negócio, dado duplicado), adicione um novo método aqui com
 * @ExceptionHandler apontando para ela.
 */

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Trata casos de recurso não encontrado (ex: buscar por ID inexistente).
     * Retorna HTTP 404.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                "Não encontrado",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * "Rede de segurança": captura qualquer exception não tratada
     * especificamente acima, evitando que o erro real vaze pro cliente
     * da API. Retorna HTTP 500 com mensagem genérica.
     *
     * Deve ficar sempre por último — handlers mais específicos têm
     * prioridade sobre este.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Erro interno",
                "Ocorreu um erro inesperado. Tente novamente mais tarde."
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
