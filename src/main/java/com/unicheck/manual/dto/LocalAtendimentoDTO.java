package com.unicheck.manual.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @apiNote Chip do bloco "Onde resolver", na entrada e na saída da API.
 * @param nome --> Nome do local; obrigatório.
 * @param url  --> Link do local; opcional.
 * @author Mateus Alves Costa
 * @since 27.09.2026
 */
public record LocalAtendimentoDTO(
    @NotBlank (message = "O nome do local é obrigatório")
    @Size (max = 100, message = "O nome do local deve ter no máximo 100 caracteres")
    String nome,
    @Size (max = 255, message = "A URL do local deve ter no máximo 255 caracteres")
    String url
) {
}
