package com.unicheck.usuario.service;

import com.unicheck.exception.ResourceNotFoundException;
import com.unicheck.usuario.domain.*;
import com.unicheck.usuario.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service @RequiredArgsConstructor
public class IdentidadeService {
    private final AlunoRepository alunoRepository;
    private final AdminRepository adminRepository;
    private final UsuarioRepository usuarioRepository;

    /** O RA vem do subject de um JWT já validado; IDs do request nunca definem a identidade ativa. */
    public Aluno alunoPorRa(String ra) {
        return alunoRepository.findByUsuario_Ra(ra)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de aluno nao encontrado"));
    }

    public Aluno bloquearAlunoPorRa(String ra) {
        return alunoRepository.bloquearPorRa(ra)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de aluno nao encontrado"));
    }

    public Admin adminPorRa(String ra) {
        Usuario usuario = usuarioRepository.findByRa(ra)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado"));
        return adminRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Perfil administrativo nao encontrado"));
    }
}
