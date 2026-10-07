package com.unicheck.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** DTO do endpoint publico POST /auth/redefinir-senha; a politica de senha e a mesma do cadastro. */
public record RedefinirSenhaRequestDTO(
        @NotBlank(message = "Token é obrigatório")
        String token,

        @NotBlank(message = "Nova senha é obrigatória")
        @Size(min = 8, message = "Nova senha deve ter no mínimo 8 caracteres")
        String novaSenha
) {
}
