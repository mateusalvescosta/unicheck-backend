package com.unicheck.beneficio.dto;

import java.util.Set;

public record BeneficioResumoDTO(
    Long id,
    String iconeUrl,
    String nome,
    String descricao,
    Set<String> categorias
) {
    
}
