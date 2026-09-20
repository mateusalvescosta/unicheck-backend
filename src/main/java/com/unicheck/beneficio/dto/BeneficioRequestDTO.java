package com.unicheck.beneficio.dto;

import java.util.List;
import java.util.Set;

import com.unicheck.beneficio.enumeration.TipoBeneficio;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * @apiNote Corpo de cadastro e de atualização do benefício.
 *
 *          As obrigatoriedades são checadas apenas no POST, que é o único a
 *          passar por Valid. No PUT, campo ausente significa "não mexe" -- quem
 *          decide isso é o BeneficioService.
 * @param iconeUrl                --> URL do ícone exibido no card; opcional.
 * @param nome                    --> Nome do benefício; obrigatório no cadastro.
 * @param chamada                 --> Frase curta do card; obrigatória no cadastro.
 * @param descricao               --> Descrição do benefício; obrigatória no cadastro.
 * @param url                     --> Link externo do benefício; obrigatório no cadastro.
 * @param tipo                    --> Tipo do benefício; obrigatório no cadastro.
 * @param publicoElegivel         --> Quem pode usar o benefício; obrigatório no cadastro.
 * @param observacaoElegibilidade --> Detalhe extra sobre elegibilidade; opcional.
 * @param avisoImportante         --> Aviso destacado no card expandido; opcional.
 * @param disponibilidade         --> Período ou condição de disponibilidade; opcional.
 * @param palavrasChave           --> Tags do card, também usadas na busca; opcional.
 * @param passos                  --> Passos do bloco "Como conseguir"; opcional.
 * @param categoriaIds            --> IDs das categorias a vincular; todas precisam
 *                                existir e estar ativas.
 * @author Mateus Alves Costa
 * @since 18.08.2026
 * @updated 19.09.2026
 */
public record BeneficioRequestDTO(
    String iconeUrl,
    @NotBlank (message = "O nome do benefício é obrigatório")
    String nome,
    @NotBlank (message = "A chamada do benefício é obrigatória")
    String chamada,
    @NotBlank (message = "A descrição do benefício é obrigatória")
    String descricao,
    @NotBlank (message = "A URL do benefício é obrigatória")
    String url,
    @NotNull (message = "O tipo do benefício é obrigatório")
    TipoBeneficio tipo,
    @NotBlank (message = "O público elegível do benefício é obrigatório")
    String publicoElegivel,
    String observacaoElegibilidade,
    String avisoImportante,
    String disponibilidade,
    Set<String> palavrasChave,
    List<@Valid PassoBeneficioDTO> passos,
    Set<Long> categoriaIds
) {
    
}
