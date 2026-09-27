package com.unicheck.manual.dto;

import com.unicheck.manual.enumeration.CategoriaManual;

/**
 * @apiNote Referência curta a outro manual, usada nos links de navegação: "Seu
 *          próximo passo", "Você também pode precisar", "Conteúdo anterior" e
 *          "Próximo conteúdo".
 * @param id            --> Identificador do manual.
 * @param titulo        --> Título do manual.
 * @param categoria     --> Constante da categoria.
 * @param categoriaNome --> Nome da categoria, como exibido no card.
 * @author Mateus Alves Costa
 * @since 27.09.2026
 */
public record ManualReferenciaDTO(
    Long id,
    String titulo,
    CategoriaManual categoria,
    String categoriaNome
) {
}
