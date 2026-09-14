package com.unicheck.checklist.dto;

import jakarta.validation.constraints.*;

public record ItemChecklistRequestDTO(
        @NotBlank @Size(max = 150) String titulo,
        @NotBlank @Size(max = 500) String descricao,
        @NotBlank String conteudo,
        @NotNull @Positive Integer ordem,
        @NotNull @PositiveOrZero Integer xp) {}
