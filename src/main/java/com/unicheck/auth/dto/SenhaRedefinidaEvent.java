package com.unicheck.auth.dto;

import java.time.OffsetDateTime;

/** Publicado na transacao da redefinicao; o aviso de "senha alterada" so sai depois do commit. */
public record SenhaRedefinidaEvent(String nome, String email, OffsetDateTime alteradaEm) {
}
