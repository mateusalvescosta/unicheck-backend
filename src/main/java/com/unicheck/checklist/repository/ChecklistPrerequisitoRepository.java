package com.unicheck.checklist.repository;

import com.unicheck.checklist.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface ChecklistPrerequisitoRepository extends JpaRepository<ChecklistPrerequisito, ChecklistPrerequisitoPK> {
    List<ChecklistPrerequisito> findByIdChecklistId(Long checklistId);
    List<ChecklistPrerequisito> findByIdChecklistRequisitoId(Long requisitoId);
    boolean existsByIdChecklistIdAndIdChecklistRequisitoId(Long checklistId, Long requisitoId);
    void deleteByIdChecklistIdAndIdChecklistRequisitoId(Long checklistId, Long requisitoId);
}
