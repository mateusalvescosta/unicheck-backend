package com.unicheck.beneficio.dto;

import java.util.Set;

import jakarta.validation.constraints.NotBlank;

public record BeneficioRequestDTO(
    String iconeUrl,
    @NotBlank (message = "O nome do benefício é obrigatório")
    String nome,
    @NotBlank (message = "A descrição do benefício é obrigatória")
    String descricao,
    @NotBlank (message = "A URL do benefício é obrigatória")
    String url,
    Set<Long> categoriaIds
) {
    
}
