package com.unicheck.beneficio.dto;

import java.util.List;
import java.util.Set;

import com.unicheck.beneficio.enumeration.TipoBeneficio;
import com.unicheck.categoria.dto.CategoriaResponseDTO;

/**
 * @apiNote Conteúdo do card expandido: tudo do resumo mais observação de
 *          elegibilidade, aviso, disponibilidade e os passos de "Como conseguir".
 *          Não traz o campo favorito, que depende do aluno autenticado e por isso
 *          só aparece na listagem.
 *
 *          A conversão a partir da entidade fica no toDto da Service, de modo
 *          que este record não precise conhecer o domínio.
 * @param id                      --> Identificador do benefício.
 * @param iconeUrl                --> URL do ícone exibido no card.
 * @param nome                    --> Nome do benefício.
 * @param chamada                 --> Frase curta do card.
 * @param descricao               --> Descrição do benefício.
 * @param url                     --> Link externo do benefício.
 * @param tipo                    --> Tipo do benefício.
 * @param publicoElegivel         --> Quem pode usar o benefício.
 * @param observacaoElegibilidade --> Detalhe extra sobre elegibilidade.
 * @param avisoImportante         --> Aviso destacado no card expandido.
 * @param disponibilidade         --> Período ou condição de disponibilidade.
 * @param palavrasChave           --> Tags do card.
 * @param passos                  --> Passos do bloco "Como conseguir", já ordenados.
 * @param categorias              --> Categorias vinculadas, ativas ou não.
 * @author Mateus Alves Costa
 * @since 18.08.2026
 * @updated 19.09.2026
 */
public record BeneficioResponseDTO(
    Long id,
    String iconeUrl,
    String nome,
    String chamada,
    String descricao,
    String url,
    TipoBeneficio tipo,
    String publicoElegivel,
    String observacaoElegibilidade,
    String avisoImportante,
    String disponibilidade,
    Set<String> palavrasChave,
    List<PassoBeneficioDTO> passos,
    Set<CategoriaResponseDTO> categorias
) {
}
