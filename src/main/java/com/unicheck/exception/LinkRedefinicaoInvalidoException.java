package com.unicheck.exception;

/** Token de redefinicao inexistente, expirado ou ja usado; a mensagem e unica para nao revelar qual dos casos ocorreu. */
public class LinkRedefinicaoInvalidoException extends RuntimeException {
    public LinkRedefinicaoInvalidoException() {
        super("Link inválido ou expirado.");
    }
}
