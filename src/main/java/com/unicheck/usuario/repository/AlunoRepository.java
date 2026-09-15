package com.unicheck.usuario.repository;

import com.unicheck.usuario.domain.Aluno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Optional;

/**
 * Repository do Aluno.
 *
 * Mesmo esquema do UsuarioRepository: extends JpaRepository<Aluno, Long>.
 */
public interface AlunoRepository extends JpaRepository<Aluno, Long> {

    Optional<Aluno> findByUsuario_Ra(String ra);

    // Caso precise achar o Aluno a partir do id do Usuario associado
    // (ex: usuário logado via JWT, e você quer os dados de Aluno dele).
    // Spring entende "By Usuario Id" como usuario.id por causa do relacionamento @OneToOne.
    Optional<Aluno> findByUsuarioId(Long usuarioId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Aluno a where a.usuario.ra = :ra")
    Optional<Aluno> bloquearPorRa(@Param("ra") String ra);

}
