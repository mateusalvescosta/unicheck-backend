package com.unicheck.beneficio.dto;

import java.util.Set;

import com.unicheck.beneficio.enumeration.TipoBeneficio;
import com.unicheck.categoria.dto.CategoriaResponseDTO;

/**
 * @apiNote Dados do card na listagem. O card expandido usa o BeneficioResponseDTO.
 *
 *          A conversão a partir da entidade fica no toDto da Service, de modo
 *          que este record não precise conhecer o domínio.
 * @param id              --> Identificador do benefício.
 * @param iconeUrl        --> URL do ícone exibido no card.
 * @param nome            --> Nome do benefício.
 * @param chamada         --> Frase curta do card.
 * @param descricao       --> Descrição do benefício.
 * @param url             --> Link externo do benefício.
 * @param tipo            --> Tipo do benefício.
 * @param publicoElegivel --> Quem pode usar o benefício.
 * @param palavrasChave   --> Tags do card.
 * @param categorias      --> Categorias vinculadas, ativas ou não.
 * @param favorito        --> Indica se o aluno autenticado favoritou o benefício.
 * @author Mateus Alves Costa
 * @since 18.08.2026
 * @updated 19.09.2026
 */
public record BeneficioSummaryDTO(
    Long id,
    String iconeUrl,
    String nome,
    String chamada,
    String descricao,
    String url,
    TipoBeneficio tipo,
    String publicoElegivel,
    Set<String> palavrasChave,
    Set<CategoriaResponseDTO> categorias,
    boolean favorito
) {
}
