package com.unicheck.exception;

/**
 * Exception lançada quando um recurso solicitado não é encontrado no banco.
 * Usada por todos os domínios (usuario, checklist, etc) para sinalizar
 * de forma padronizada que um ID buscado não existe.
 *
 * Exemplo de uso no service:
 *   Aluno aluno = alunoRepository.findById(id)
 *       .orElseThrow(() -> new ResourceNotFoundException("Aluno", id));
 *
 * É capturada automaticamente pelo GlobalExceptionHandler, que devolve
 * um HTTP 404 com corpo JSON padronizado.
 */

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException (String resource, Long id){
        super(resource + " não encontrado(a) com id: " + id);
    }
}
