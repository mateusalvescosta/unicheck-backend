package com.unicheck.categoria.dto;

/**
 * @apiNote Dados da categoria devolvidos pela API, inclusive dentro dos
 *          benefícios. O campo ativo vem junto para que a tela consiga marcar
 *          uma categoria desativada que um benefício antigo ainda usa.
 *
 *          A conversão a partir da entidade fica no toDto da Service, de modo
 *          que este record não precise conhecer o domínio.
 * @param id    --> Identificador da categoria.
 * @param nome  --> Nome da categoria.
 * @param ativo --> Indica se a categoria pode ser vinculada a novos benefícios.
 * @author Mateus Alves Costa
 * @since 18.08.2026
 * @updated 19.09.2026
 */
public record CategoriaResponseDTO(
    Long id,
    String nome,
    boolean ativo
) {
}
