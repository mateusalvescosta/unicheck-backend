package com.unicheck.notificacao.dto;

import jakarta.validation.constraints.NotNull;

/** Corpo de PUT /notificacoes/preferencias/{tipo}; o tipo vem na URL. */
public record AlterarPreferenciaRequestDTO(
        @NotNull(message = "Ativo é obrigatório")
        Boolean ativo
) {
}
