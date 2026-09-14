package com.unicheck.checklist.dto;

import com.unicheck.checklist.enumeration.*;
import java.util.List;

public record ChecklistAlunoResponseDTO(Long id, String nome, String descricao, Integer ordem,
        StatusChecklist statusEditorial, EstadoChecklistAluno estado, String motivoBloqueio,
        int quantidadeEtapas, long etapasConcluidas, int percentualProgresso,
        int xpEtapasPossivel, int bonusFinal, int xpTotalPossivel, int horasComplementares,
        Long proximaEtapaId, List<PrerequisitoResponseDTO> prerequisitos,
        List<ItemChecklistResponseDTO> etapas) {}
