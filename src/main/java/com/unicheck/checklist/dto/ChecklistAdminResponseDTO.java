package com.unicheck.checklist.dto;

import com.unicheck.checklist.enumeration.StatusChecklist;
import java.util.List;

public record ChecklistAdminResponseDTO(Long id, String nome, String descricao, Integer ordem,
        Integer bonusFinal, Integer xpEtapasPossivel, Integer xpTotalPossivel,
        Integer horasComplementares, StatusChecklist status, boolean possuiProgresso,
        List<Long> prerequisitoIds, List<ItemChecklistResponseDTO> etapas) {}
