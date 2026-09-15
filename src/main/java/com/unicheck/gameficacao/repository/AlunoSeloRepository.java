package com.unicheck.gameficacao.repository;
import com.unicheck.gameficacao.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface AlunoSeloRepository extends JpaRepository<AlunoSelo, AlunoSeloPK> {
    boolean existsByIdAlunoIdAndIdSeloId(Long alunoId, Long seloId);
    List<AlunoSelo> findByIdAlunoIdOrderByDataConquista(Long alunoId);
}
