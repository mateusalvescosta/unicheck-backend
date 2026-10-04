package com.unicheck.manual.dto;

import com.unicheck.manual.enumeration.CategoriaManual;

/**
 * @apiNote Card da listagem e dos resultados da busca. A página do manual usa o
 *          ManualResponseDTO.
 * @param id            --> Identificador do manual.
 * @param ordem         --> Posição do manual na sequência.
 * @param categoria     --> Constante da categoria.
 * @param categoriaNome --> Nome da categoria, como exibido no card.
 * @param titulo        --> Título do manual.
 * @param resumo        --> Frase abaixo do título.
 * @param tempoLeitura  --> Tempo estimado de leitura, em minutos.
 * @author Mateus Alves Costa
 * @since 27.09.2026
 */
public record ManualSummaryDTO(
    Long id,
    Integer ordem,
    CategoriaManual categoria,
    String categoriaNome,
    String titulo,
    String resumo,
    Integer tempoLeitura
) {
}
