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
import com.unicheck.usuario.util.SenhaTemporariaUtil;
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
    private final AdminService adminService;
    private final UsuarioValidacaoService usuarioValidacaoService;

    @Transactional
    public UsuarioResponseDTO criar(UsuarioRequestDTO dto) {
        // A verificacao antecipada transforma unicidade de RA/email em erro de negocio claro; o banco continua sendo a garantia final.
        usuarioValidacaoService.validarUnicidade(dto.ra(), dto.email());
        if (dto.role() == RoleUsuario.ADMIN && (dto.cargo() == null || dto.cargo().isBlank())) {
            // cargo e obrigatorio em tb_admin; validar antes de persistir evita iniciar uma composicao invalida.
            throw new ResourceConflictException("Cargo e obrigatorio para usuario ADMIN");
        }
        Usuario usuario = usuarioMapper.toEntity(dto);
        if (usuario.getRole() == RoleUsuario.ADMIN) {
            // Admin segue a mesma regra de primeiro acesso do endpoint especifico: senha temporaria e troca obrigatoria.
            usuario.setSenha(passwordEncoder.encode(SenhaTemporariaUtil.gerar(dto.dataNascimento())));
            usuario.setTrocarSenhaNoLogin(true);
        } else {
            usuario.setSenha(passwordEncoder.encode(dto.senha()));
            usuario.setTrocarSenhaNoLogin(false);
        }
        usuarioRepository.save(usuario);

        if (usuario.getRole() == RoleUsuario.ADMIN) {
            adminService.criarRegistro(usuario, dto.cargo());
        }
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
