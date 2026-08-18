package com.unicheck.beneficio.dto;

import java.util.Set;

import com.unicheck.categoria.dto.CategoriaResponseDTO;

public record BeneficioResponseDTO(
    Long id,
    String iconeUrl,
    String nome,
    String descricao,
    String url,
    Set<CategoriaResponseDTO> categorias
) {

    
}
