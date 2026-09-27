package com.unicheck.manual.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * @apiNote Item de "Atalhos do dia a dia", na entrada e na saída da API. Não
 *          tem campo de ordem: a posição do item na lista já é a ordem da grade.
 * @param manualId --> Manual aberto pelo atalho; obrigatório.
 * @param nome     --> Nome do atalho, como "Emitir boleto"; obrigatório.
 * @author Mateus Alves Costa
 * @since 27.09.2026
 */
public record AtalhoManualDTO(
    @NotNull (message = "O manual do atalho é obrigatório")
    Long manualId,
    @NotBlank (message = "O nome do atalho é obrigatório")
    @Size (max = 60, message = "O nome do atalho deve ter no máximo 60 caracteres")
    String nome
) {
}
