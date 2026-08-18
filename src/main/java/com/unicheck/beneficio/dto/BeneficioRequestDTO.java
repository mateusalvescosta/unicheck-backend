package com.unicheck.beneficio.dto;

import java.util.Set;

import com.unicheck.beneficio.domain.Categoria;

public record BeneficioRequestDTO(
    String iconeUrl,
    String nome,
    String descricao,
    String url,
    Set<Categoria> categorias
) {
    
}
