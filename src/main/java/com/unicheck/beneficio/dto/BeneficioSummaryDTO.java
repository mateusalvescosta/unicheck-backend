package com.unicheck.beneficio.dto;

import java.util.Set;

import com.unicheck.categoria.dto.CategoriaResponseDTO;

public record BeneficioSummaryDTO(
    Long id,
    String iconeUrl,
    String nome,
    String descricao,
    Set<CategoriaResponseDTO> categorias
) {
    
}
