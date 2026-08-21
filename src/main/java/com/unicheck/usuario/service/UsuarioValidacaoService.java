package com.unicheck.usuario.service;

import com.unicheck.exception.ResourceConflictException;
import com.unicheck.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Centraliza validacoes de Usuario usadas por fluxos diferentes para que cadastro
 * de aluno, criacao de Admin e CRUD administrativo devolvam o mesmo erro de unicidade.
 */
@Service
@RequiredArgsConstructor
class UsuarioValidacaoService {
    private final UsuarioRepository usuarioRepository;

    void validarUnicidade(String ra, String email) {
        // O banco continua sendo a garantia contra concorrencia; aqui evitamos expor detalhes do PostgreSQL ao cliente.
        if (usuarioRepository.existsByRa(ra)) {
            throw new ResourceConflictException("RA ja cadastrado");
        }
        if (usuarioRepository.existsByEmail(email)) {
            throw new ResourceConflictException("Email ja cadastrado");
        }
    }
}
