package com.unicheck.checklist.dto;

import jakarta.validation.constraints.*;

public record ChecklistRequestDTO(
        @NotBlank @Size(max = 150) String nome,
        @NotBlank @Size(max = 500) String descricao,
        @NotNull @Positive Integer ordem,
        @NotNull @PositiveOrZero Integer xp,
        @NotNull @PositiveOrZero Integer horasComplementares) {}
