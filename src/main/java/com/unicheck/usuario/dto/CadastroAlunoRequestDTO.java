package com.unicheck.usuario.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO de auto-cadastro do Aluno (tela pública).
 * Não tem "role" -- a Service sempre fixa RoleUsuario.ALUNO, nunca vem do cliente.
 * Não tem "trocarSenhaNoLogin" -- só é true (senha temporária) quando OUTRA
 * pessoa cria o usuário por você (caso do Admin). Aqui o próprio aluno
 * escolheu a senha, então não precisa trocar depois.
 */
public record CadastroAlunoRequestDTO(
        @NotBlank(message = "Nome é obrigatório")
        String nome,

        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email inválido")
        String email,

        @NotBlank(message = "Senha é obrigatória")
        @Size(min = 8, message = "Senha deve ter no mínimo 8 caracteres")
        String senha,

        @NotBlank(message = "RA é obrigatório")
        String ra,

        @NotNull(message = "Curso é obrigatório")
        Long cursoId,

        @NotBlank(message = "Período é obrigatório")
        String periodo
) {
}