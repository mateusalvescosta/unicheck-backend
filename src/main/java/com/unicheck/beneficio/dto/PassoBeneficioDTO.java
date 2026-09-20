package com.unicheck.beneficio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * @apiNote Passo do bloco "Como conseguir" do card expandido, na entrada e na
 *          saída da API.
 *
 *          A conversão a partir da entidade fica no toDto da Service, de modo
 *          que este record não precise conhecer o domínio.
 * @param ordem     --> Posição do passo na sequência; obrigatória e maior que zero.
 * @param titulo    --> Título do passo; obrigatório.
 * @param descricao --> Descrição do passo; obrigatória.
 * @author Mateus Alves Costa
 * @since 18.08.2026
 * @updated 19.09.2026
 */
public record PassoBeneficioDTO(
    @NotNull (message = "A ordem do passo é obrigatória")
    @Positive (message = "A ordem do passo deve ser maior que zero")
    Integer ordem,
    @NotBlank (message = "O título do passo é obrigatório")
    String titulo,
    @NotBlank (message = "A descrição do passo é obrigatória")
    String descricao
) {
}
