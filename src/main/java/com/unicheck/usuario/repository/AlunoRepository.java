package com.unicheck.usuario.repository;

import com.unicheck.usuario.domain.Aluno;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository do Aluno.
 *
 * Mesmo esquema do UsuarioRepository: extends JpaRepository<Aluno, Long>.
 */
public interface AlunoRepository extends JpaRepository<Aluno, Long> {

    // RA é único (@Column(unique = true)), então é o principal ponto de busca
    // além do id -- ex: consultar o aluno pelo RA na tela de checklist.
    Optional<Aluno> findByRa(String ra);

    // Caso precise achar o Aluno a partir do id do Usuario associado
    // (ex: usuário logado via JWT, e você quer os dados de Aluno dele).
    // Spring entende "By Usuario Id" como usuario.id por causa do relacionamento @OneToOne.
    Optional<Aluno> findByUsuarioId(Long usuarioId);

    boolean existsByRa(String ra);
}
