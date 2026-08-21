package com.unicheck.usuario.dto;

import com.unicheck.usuario.enumeration.RoleUsuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Dados para o CRUD administrativo de Usuario. Senha nunca volta na resposta.
 */
public record UsuarioRequestDTO(
        @NotBlank(message = "Nome e obrigatorio!")
        String nome,

        @Size(min = 8, message = "Senha deve conter no minimo 8 caracteres!")
        String senha,

        @NotBlank(message = "RA e obrigatorio!")
        String ra,

        @NotNull(message = "Data de nascimento e obrigatoria!")
        LocalDate dataNascimento,

        @NotNull(message = "Role e obrigatoria!")
        RoleUsuario role,

        String cargo,

        String fotoUrl,

        @NotBlank(message = "Email e obrigatorio!") @Email(message = "Email invalido!")
        String email
) {
}
