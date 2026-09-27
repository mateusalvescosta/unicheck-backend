package com.unicheck.manual.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * @apiNote Lista completa de "Atalhos do dia a dia", na ordem de exibição. Lista
 *          vazia remove todos os atalhos.
 * @param atalhos --> Atalhos a exibir; no máximo oito, a grade da tela.
 * @author Mateus Alves Costa
 * @since 27.09.2026
 */
public record AtalhosManualRequestDTO(
    @NotNull (message = "A lista de atalhos é obrigatória")
    @Size (max = 8, message = "São permitidos no máximo 8 atalhos")
    List<@NotNull @Valid AtalhoManualDTO> atalhos
) {
}
