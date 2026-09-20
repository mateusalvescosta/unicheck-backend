package com.unicheck.gameficacao.repository;
import com.unicheck.gameficacao.domain.Nivel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface NivelRepository extends JpaRepository<Nivel, Long> {
    List<Nivel> findByXpMinimoLessThanEqualOrderByXpMinimoAsc(Integer xp);
}
