package com.unicheck.usuario.dto;

import com.unicheck.usuario.enumeration.RoleUsuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Dados para o CRUD administrativo de Usuario.
 *
 * Este endpoint (/usuarios) so cria usuarios com role ALUNO -- a criacao de
 * ADMIN e responsabilidade exclusiva de POST /admins (CriarAdminRequestDTO),
 * que gera a senha temporaria no servidor e nao aceita senha vinda do cliente.
 * Isso evita ter dois caminhos diferentes fazendo a mesma coisa de formas
 * distintas. Por isso "senha" e obrigatoria aqui (sempre e o proprio Admin
 * escolhendo a senha inicial do Aluno) e o campo "cargo" foi removido, ja
 * que so fazia sentido no fluxo de ADMIN.
 *
 * Senha nunca volta na resposta.
 */
public record UsuarioRequestDTO(
        @NotBlank(message = "Nome e obrigatorio!")
        String nome,

        @NotBlank(message = "Senha e obrigatoria!")
        @Size(min = 8, message = "Senha deve conter no minimo 8 caracteres!")
        String senha,

        @NotBlank(message = "RA e obrigatorio!")
        String ra,

        @NotNull(message = "Data de nascimento e obrigatoria!")
        LocalDate dataNascimento,

        @NotNull(message = "Role e obrigatoria!")
        RoleUsuario role,

        String fotoUrl,

        @NotBlank(message = "Email e obrigatorio!") @Email(message = "Email invalido!")
        String email
) {
}