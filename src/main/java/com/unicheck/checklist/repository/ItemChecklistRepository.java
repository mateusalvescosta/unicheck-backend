package com.unicheck.checklist.repository;

import com.unicheck.checklist.domain.ItemChecklist;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface ItemChecklistRepository extends JpaRepository<ItemChecklist, Long> {
    List<ItemChecklist> findByChecklistIdOrderByOrdem(Long checklistId);
    boolean existsByChecklistIdAndOrdem(Long checklistId, Integer ordem);
}
