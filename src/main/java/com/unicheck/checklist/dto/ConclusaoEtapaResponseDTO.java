package com.unicheck.checklist.dto;

import java.time.OffsetDateTime;

public record ConclusaoEtapaResponseDTO(Long checklistId, Long itemId, OffsetDateTime dataConclusao,
        Integer xpEtapaCreditado, boolean checklistConcluido, Integer bonusFinalCreditado,
        Integer horasConquistadas, Integer xpTotalAluno, boolean repetida) {}
