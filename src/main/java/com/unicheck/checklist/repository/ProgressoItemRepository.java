package com.unicheck.checklist.repository;

import com.unicheck.checklist.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface ProgressoItemRepository extends JpaRepository<ProgressoItem, ProgressoItemPK> {
    Optional<ProgressoItem> findByIdAlunoIdAndIdItemId(Long alunoId, Long itemId);
    List<ProgressoItem> findByIdAlunoIdAndIdItemChecklistId(Long alunoId, Long checklistId);
    boolean existsByIdItemChecklistId(Long checklistId);
    boolean existsByIdAlunoIdAndIdItemChecklistId(Long alunoId, Long checklistId);
}
