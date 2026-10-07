package com.unicheck.auth.repository;

import com.unicheck.auth.domain.TokenRedefinicaoSenha;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface TokenRedefinicaoSenhaRepository extends JpaRepository<TokenRedefinicaoSenha, Long> {

    Optional<TokenRedefinicaoSenha> findByTokenHash(String tokenHash);

    long countByUsuarioIdAndCriadoEmAfter(Long usuarioId, OffsetDateTime desde);

    // Marca como usados os tokens ainda pendentes; usado ao pedir um novo link e ao concluir a redefinicao.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update TokenRedefinicaoSenha t set t.usadoEm = :agora "
            + "where t.usuario.id = :usuarioId and t.usadoEm is null")
    int invalidarPendentes(@Param("usuarioId") Long usuarioId, @Param("agora") OffsetDateTime agora);
}
