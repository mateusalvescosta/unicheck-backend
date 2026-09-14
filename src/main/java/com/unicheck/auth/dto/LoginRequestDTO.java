package com.unicheck.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO de ENTRADA do login (endpoint POST /auth/login).
 *
 * Login é feito só com RA + senha -- vale tanto pra Aluno quanto pra Admin,
 * já que o campo "ra" mora na entidade Usuario (compartilhada pelos dois).
 * Não existe mais login por email -- email hoje é só dado de contato.
 *
 * Esse DTO não vira entidade e não passa por Mapper: a Service usa o "ra"
 * pra autenticar via AuthenticationManager (Spring Security), que por trás
 * dos panos busca o Usuario pelo RA e compara a senha com BCrypt.
 */
public record LoginRequestDTO(
        @NotBlank(message = "RA é obrigatório!")
        String ra,

        @NotBlank(message = "Senha é obrigatória!")
        String senha
) {
}
