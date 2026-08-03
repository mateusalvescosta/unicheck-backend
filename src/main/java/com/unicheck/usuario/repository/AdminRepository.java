package com.unicheck.usuario.repository;

import com.unicheck.usuario.domain.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository do Admin.
 *
 * Mesmo esquema dos outros: extends JpaRepository<Admin, Long>.
 */
public interface AdminRepository extends JpaRepository<Admin, Long> {

    // Igual ao findByUsuarioId do AlunoRepository: acha o Admin
    // a partir do id do Usuario associado (@OneToOne).
    Optional<Admin> findByUsuarioId(Long usuarioId);
}
