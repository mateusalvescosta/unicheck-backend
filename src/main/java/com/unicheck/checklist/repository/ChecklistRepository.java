package com.unicheck.checklist.repository;

import com.unicheck.checklist.domain.Checklist;
import com.unicheck.checklist.enumeration.StatusChecklist;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface ChecklistRepository extends JpaRepository<Checklist, Long> {
    @EntityGraph(attributePaths = "itens")
    List<Checklist> findAllByStatusOrderByOrdem(StatusChecklist status);

    @EntityGraph(attributePaths = "itens")
    @Query("select c from Checklist c where c.id = :id")
    Optional<Checklist> buscarDetalhado(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Checklist c where c.id = :id")
    Optional<Checklist> bloquearPorId(@Param("id") Long id);

    /** Serializa mudanças no grafo para que duas validações concorrentes não criem um ciclo. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Checklist c order by c.id")
    List<Checklist> bloquearTodos();
}
