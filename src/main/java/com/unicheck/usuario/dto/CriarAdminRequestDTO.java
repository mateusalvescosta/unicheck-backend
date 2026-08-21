package com.unicheck.usuario.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * DTO usado por um Admin já logado pra criar outro Admin.
 *
 * "senha" aqui é uma senha TEMPORÁRIA -- quem cria escolhe (ou o sistema
 * podia gerar aleatória, mas por simplicidade o Admin define). A Service
 * salva o Usuario já com trocarSenhaNoLogin = true, forçando o novo Admin
 * a trocar assim que logar pela primeira vez (Opção 2 que decidimos).
 *
 * Endpoint que recebe isso precisa estar protegido só pra ADMIN
 * (@PreAuthorize ou regra equivalente no SecurityConfig -- isso não é
 * responsabilidade do DTO, é do Controller/Security).
 */
public record CriarAdminRequestDTO(
        @NotBlank(message = "Nome é obrigatório")
        String nome,

        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email inválido")
        String email,

        @NotBlank(message = "RA é obrigatório")
        String ra,

        @NotNull(message = "Data de nascimento é obrigatória")
        LocalDate dataNascimento,

        @NotBlank(message = "Cargo é obrigatório")
        String cargo
) {
}
