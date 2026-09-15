package com.unicheck.checklist.repository;

import com.unicheck.checklist.domain.*;
import com.unicheck.checklist.enumeration.StatusLancamentoHoras;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface ChecklistConcluidoRepository extends JpaRepository<ChecklistConcluido, ChecklistConcluidoPK> {
    boolean existsByIdAlunoIdAndIdChecklistId(Long alunoId, Long checklistId);
    boolean existsByIdChecklistId(Long checklistId);
    List<ChecklistConcluido> findByIdAlunoIdOrderByDataConclusaoDesc(Long alunoId);
    List<ChecklistConcluido> findAllByOrderByDataConclusaoDesc();
    List<ChecklistConcluido> findByStatusLancamentoHorasOrderByDataConclusaoAsc(StatusLancamentoHoras status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ChecklistConcluido c where c.id.aluno.id = :alunoId and c.id.checklist.id = :checklistId")
    Optional<ChecklistConcluido> bloquear(@Param("alunoId") Long alunoId, @Param("checklistId") Long checklistId);
}
