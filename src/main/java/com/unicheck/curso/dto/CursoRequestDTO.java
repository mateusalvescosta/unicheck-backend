package com.unicheck.curso.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CursoRequestDTO(

        @NotBlank(message = "O campo nome é obrigatório.")
        @Size(max = 100, message = "O campo nome deve ter no máximo 100 caracteres.")
        String nome
) {
}
