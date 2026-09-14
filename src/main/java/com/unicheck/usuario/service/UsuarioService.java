package com.unicheck.usuario.service;

import com.unicheck.exception.ResourceNotFoundException;
import com.unicheck.exception.ResourceConflictException;
import com.unicheck.usuario.domain.Usuario;
import com.unicheck.usuario.dto.TrocarSenhaRequestDTO;
import com.unicheck.usuario.dto.UsuarioRequestDTO;
import com.unicheck.usuario.dto.UsuarioResponseDTO;
import com.unicheck.usuario.mapper.UsuarioMapper;
import com.unicheck.usuario.repository.UsuarioRepository;
import com.unicheck.usuario.enumeration.RoleUsuario;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper usuarioMapper;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioValidacaoService usuarioValidacaoService;

    @Transactional
    public UsuarioResponseDTO criar(UsuarioRequestDTO dto) {
        // Unico caminho para criar ADMIN e POST /admins (AdminService.criar): la a senha temporaria
        // e gerada no servidor e a troca no primeiro login e obrigatoria. Duplicar essa regra aqui
        // criava dois fluxos divergentes para o mesmo caso de uso, entao esse endpoint agora so aceita ALUNO.
        if (dto.role() != RoleUsuario.ALUNO) {
            throw new ResourceConflictException("Use POST /admins para criar usuarios ADMIN");
        }
        // A verificacao antecipada transforma unicidade de RA/email em erro de negocio claro; o banco continua sendo a garantia final.
        usuarioValidacaoService.validarUnicidade(dto.ra(), dto.email());
        Usuario usuario = usuarioMapper.toEntity(dto);
        usuario.setSenha(passwordEncoder.encode(dto.senha()));
        usuario.setTrocarSenhaNoLogin(false);
        usuarioRepository.save(usuario);
        return usuarioMapper.toResponseDTO(usuario);
    }

    @Transactional(readOnly = true)
    public UsuarioResponseDTO buscarPorId(Long id) {
        return usuarioMapper.toResponseDTO(buscarEntidade(id));
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponseDTO> listar() {
        return usuarioRepository.findAll().stream().map(usuarioMapper::toResponseDTO).toList();
    }

    @Transactional
    public void deletar(Long id) {
        // Busca antes de excluir para devolver 404 consistente em vez de um delete silencioso.
        usuarioRepository.delete(buscarEntidade(id));
    }

    @Transactional
    public void trocarSenha(String raAutenticado, TrocarSenhaRequestDTO dto) {
        // O RA vem do JWT, nao do corpo, para impedir que um usuario troque a senha de outra conta.
        Usuario usuario = usuarioRepository.findByRa(raAutenticado)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado"));
        if (!passwordEncoder.matches(dto.senhaAtual(), usuario.getSenha())) {
            // O token pode ter sido obtido por sessao esquecida em outro dispositivo.
            throw new AccessDeniedException("Senha atual incorreta");
        }
        usuario.setSenha(passwordEncoder.encode(dto.novaSenha()));
        usuario.setTrocarSenhaNoLogin(false);
    }

    Usuario buscarEntidade(Long id) {
        return usuarioRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Usuario", id));
    }

}