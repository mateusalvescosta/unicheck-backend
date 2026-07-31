package com.unicheck.exception;

import java.time.LocalDateTime;

/**
 * Formato padrão de resposta de erro devolvido pela API.
 * Usado pelo GlobalExceptionHandler para que toda exception vire
 * um JSON consistente no corpo da resposta, independente de qual
 * controller/domínio originou o erro.
 *
 * Exemplo de JSON gerado:
 * {
 *   "timestamp": "2026-07-30T14:00:00",
 *   "status": 404,
 *   "error": "Não encontrado",
 *   "message": "Curso não encontrado(a) com id: 5"
 * }
 */

public record ErrorResponse (
    LocalDateTime timestamp,
    int status,
    String error,
    String message
) {}
