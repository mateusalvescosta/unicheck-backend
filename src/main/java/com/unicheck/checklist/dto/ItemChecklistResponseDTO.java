package com.unicheck.checklist.dto;

import com.unicheck.checklist.enumeration.StatusProgressoItem;
import java.time.OffsetDateTime;

public record ItemChecklistResponseDTO(Long id, String titulo, String descricao, String conteudo,
        Integer ordem, Integer xp, StatusProgressoItem status, OffsetDateTime dataConclusao,
        Integer xpCreditado, boolean proximaPermitida) {}
