package com.unicheck.manual.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.unicheck.manual.domain.AtalhoManual;

/**
 * @apiNote Repository que lida com o acesso a dados de "Atalhos do dia a dia".
 * @author Mateus Alves Costa
 * @since 27.09.2026
 */
public interface AtalhoManualRepository extends JpaRepository<AtalhoManual, Long> {

    /**
     * @apiNote Lista os atalhos na ordem da grade, já com o manual carregado para
     *          evitar uma consulta por atalho.
     * @return --> Retorna os atalhos ordenados.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @EntityGraph(attributePaths = "manual")
    List<AtalhoManual> findAllByOrderByOrdemAsc();
}
