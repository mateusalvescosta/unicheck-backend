package com.unicheck.categoria.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * @apiNote Corpo de cadastro e de atualização da categoria. Não carrega o campo
 *          ativo: o estado é controlado pelos endpoints de ativar e desativar,
 *          nunca pelo corpo de um PUT.
 * @param nome --> Nome da categoria; obrigatório.
 * @author Mateus Alves Costa
 * @since 18.08.2026
 * @updated 19.09.2026
 */
public record CategoriaRequestDTO(
    @NotBlank(message = "O nome da categoria é obrigatório")
    String nome
) {
    
}
