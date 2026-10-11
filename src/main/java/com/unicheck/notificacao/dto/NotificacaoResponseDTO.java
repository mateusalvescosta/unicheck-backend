package com.unicheck.notificacao.dto;

import com.unicheck.beneficio.dto.BeneficioSummaryDTO;
import com.unicheck.checklist.dto.ChecklistAlunoResponseDTO;
import com.unicheck.manual.dto.ManualSummaryDTO;
import com.unicheck.notificacao.enumeration.TipoNotificacao;

import java.time.OffsetDateTime;

/**
 * Só o campo do objeto correspondente ao tipo vem preenchido; os outros dois vêm nulos.
 * Campos separados, em vez de um genérico, deixam o formato de cada objeto documentado no Swagger.
 */
public record NotificacaoResponseDTO(
        Long id,
        TipoNotificacao tipo,
        String mensagem,
        Boolean lida,
        OffsetDateTime criadoEm,
        BeneficioSummaryDTO beneficio,
        ChecklistAlunoResponseDTO checklist,
        ManualSummaryDTO manual
) {
}
