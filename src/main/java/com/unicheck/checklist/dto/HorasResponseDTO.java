package com.unicheck.checklist.dto;

import com.unicheck.checklist.enumeration.StatusLancamentoHoras;
import java.time.OffsetDateTime;

public record HorasResponseDTO(Long alunoId, String alunoNome, Long checklistId, String checklistNome,
        Integer horasConquistadas, StatusLancamentoHoras status, OffsetDateTime dataConclusao,
        OffsetDateTime dataLancamento, Long adminResponsavelId) {}
