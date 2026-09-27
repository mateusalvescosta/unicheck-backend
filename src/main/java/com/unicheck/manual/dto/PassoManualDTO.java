package com.unicheck.manual.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * @apiNote Passo numerado do bloco "Como fazer", na entrada e na saída da API.
 * @param ordem     --> Posição do passo na sequência; obrigatória e maior que zero.
 * @param descricao --> Texto do passo; obrigatório.
 * @author Mateus Alves Costa
 * @since 27.09.2026
 */
public record PassoManualDTO(
    @NotNull (message = "A ordem do passo é obrigatória")
    @Positive (message = "A ordem do passo deve ser maior que zero")
    Integer ordem,
    @NotBlank (message = "A descrição do passo é obrigatória")
    @Size (max = 255, message = "A descrição do passo deve ter no máximo 255 caracteres")
    String descricao
) {
}
