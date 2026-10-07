package com.unicheck.auth.dto;

/**
 * Publicado dentro da transacao que grava o token; o envio so acontece depois do commit,
 * para o e-mail nunca sair antes de o token existir no banco. O token em claro vive apenas neste evento.
 */
public record RecuperacaoSenhaSolicitadaEvent(String nome, String email, String token, long validadeMinutos) {
}
