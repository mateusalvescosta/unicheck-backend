package com.unicheck.manual.dto;

import com.unicheck.manual.enumeration.CategoriaManual;

/**
 * @apiNote Card de "Encontre por assunto" e chip de filtro da listagem.
 * @param codigo            --> Constante da categoria, usada no filtro.
 * @param nome              --> Nome exibido no card.
 * @param nomeCurto         --> Nome exibido no chip de filtro.
 * @param descricao         --> Texto de apoio do card.
 * @param quantidadeManuais --> Quantidade de manuais ("9 orientações").
 * @author Mateus Alves Costa
 * @since 27.09.2026
 */
public record CategoriaManualDTO(
    CategoriaManual codigo,
    String nome,
    String nomeCurto,
    String descricao,
    long quantidadeManuais
) {
}
