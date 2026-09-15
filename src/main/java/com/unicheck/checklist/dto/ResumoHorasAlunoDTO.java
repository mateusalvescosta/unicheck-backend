package com.unicheck.checklist.dto;
import java.util.List;
public record ResumoHorasAlunoDTO(int horasConquistadas, int horasPendentes, int horasLancadas,
        List<HorasResponseDTO> registros) {}
