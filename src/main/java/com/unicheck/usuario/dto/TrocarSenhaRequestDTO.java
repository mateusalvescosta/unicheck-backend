package com.unicheck.usuario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO do endpoint PATCH /usuarios/trocar-senha (usuário já autenticado).
 * Pede a senha atual por segurança -- mesmo com token válido, é boa prática
 * confirmar que quem está trocando sabe a senha "de agora" antes de definir
 * a nova (evita que alguém com sessão aberta em outro dispositivo troque
 * sem o dono perceber).
 */
public record TrocarSenhaRequestDTO(
        @NotBlank(message = "Senha atual é obrigatória")
        String senhaAtual,

        @NotBlank(message = "Nova senha é obrigatória")
        @Size(min = 8, message = "Nova senha deve ter no mínimo 8 caracteres")
        String novaSenha
) {
}