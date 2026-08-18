package com.unicheck.beneficio.dto;

import java.util.Set;

public record BeneficioDetalheDTO(
    Long id,
    String iconeUrl,
    String nome,
    String descricao,
    String url,
    Set<String> categorias
) {

    
}
