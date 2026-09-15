package com.unicheck.usuario.service;

import com.unicheck.exception.ResourceConflictException;
import com.unicheck.usuario.domain.Usuario;
import com.unicheck.usuario.dto.TrocarSenhaRequestDTO;
import com.unicheck.usuario.dto.UsuarioRequestDTO;
import com.unicheck.usuario.enumeration.RoleUsuario;
import com.unicheck.usuario.mapper.UsuarioMapper;
import com.unicheck.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * AdminService saiu das dependencias de UsuarioService: a criacao de ADMIN agora
 * e responsabilidade exclusiva de AdminService/POST /admins (ver AdminServiceTest).
 * Por isso os cenarios antigos de "criar admin via /usuarios" foram substituidos
 * pelo teste que garante que esse endpoint rejeita qualquer role diferente de ALUNO.
 */
@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private UsuarioMapper usuarioMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private UsuarioValidacaoService usuarioValidacaoService;
    @InjectMocks private UsuarioService usuarioService;

    @Test
    void criarAlunoValidoSalvaUsuarioComSenhaCriptografadaESemTrocaObrigatoria() {
        UsuarioRequestDTO dto = dto(RoleUsuario.ALUNO);
        Usuario usuario = usuario(RoleUsuario.ALUNO);
        when(usuarioMapper.toEntity(dto)).thenReturn(usuario);
        when(passwordEncoder.encode("senha-segura")).thenReturn("hash");

        usuarioService.criar(dto);

        verify(usuarioValidacaoService).validarUnicidade("RA001", "email@teste.com");
        verify(usuarioRepository).save(usuario);
        assertEquals("hash", usuario.getSenha());
        assertFalse(usuario.getTrocarSenhaNoLogin());
    }

    @Test
    void criarComRoleAdminLancaResourceConflictAntesDeQualquerValidacaoOuPersistencia() {
        // Este endpoint so cria ALUNO; criar ADMIN e exclusivo de POST /admins (AdminService).
        UsuarioRequestDTO dto = dto(RoleUsuario.ADMIN);

        assertThrows(ResourceConflictException.class, () -> usuarioService.criar(dto));

        verifyNoInteractions(usuarioValidacaoService, usuarioMapper, usuarioRepository, passwordEncoder);
    }

    @Test
    void trocarSenhaComSenhaAtualCorretaAtualizaHashELiberaUsuario() {
        Usuario usuario = usuario(RoleUsuario.ALUNO);
        usuario.setSenha("hash-antigo");
        usuario.setTrocarSenhaNoLogin(true);
        when(usuarioRepository.findByRa("RA001")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senha-antiga", "hash-antigo")).thenReturn(true);
        when(passwordEncoder.encode("senha-nova")).thenReturn("hash-novo");

        usuarioService.trocarSenha("RA001", new TrocarSenhaRequestDTO("senha-antiga", "senha-nova"));

        assertEquals("hash-novo", usuario.getSenha());
        assertFalse(usuario.getTrocarSenhaNoLogin());
    }

    @Test
    void trocarSenhaComSenhaAtualIncorretaRetornaAcessoNegado() {
        Usuario usuario = usuario(RoleUsuario.ALUNO);
        usuario.setSenha("hash-antigo");
        when(usuarioRepository.findByRa("RA001")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("errada", "hash-antigo")).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> usuarioService.trocarSenha("RA001", new TrocarSenhaRequestDTO("errada", "senha-nova")));
    }

    // Nao ha mais teste de "senha em branco" na Service: apos a mudanca, isso e um
    // @NotBlank no UsuarioRequestDTO, validado pelo @Valid no Controller antes de a
    // requisicao sequer chegar aqui. Cobrir isso em teste de Service reimplementaria
    // o Bean Validation na mao; o lugar certo pra esse caso e um teste de integracao
    // do Controller (ex.: MockMvc esperando 400) ou um teste direto do DTO com um
    // jakarta.validation.Validator.

    private UsuarioRequestDTO dto(RoleUsuario role) {
        return new UsuarioRequestDTO("Nome", "senha-segura", "RA001",
                LocalDate.of(2000, 1, 1), role, null, "email@teste.com");
    }

    private Usuario usuario(RoleUsuario role) {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setRa("RA001");
        usuario.setRole(role);
        return usuario;
    }
}