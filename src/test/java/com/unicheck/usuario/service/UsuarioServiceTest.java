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

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private UsuarioMapper usuarioMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AdminService adminService;
    @Mock private UsuarioValidacaoService usuarioValidacaoService;
    @InjectMocks private UsuarioService usuarioService;

    @Test
    void criarAlunoNaoCriaRegistroAdmin() {
        UsuarioRequestDTO dto = dto(RoleUsuario.ALUNO, null);
        Usuario usuario = usuario(RoleUsuario.ALUNO);
        when(usuarioMapper.toEntity(dto)).thenReturn(usuario);
        when(passwordEncoder.encode("senha-segura")).thenReturn("hash");

        usuarioService.criar(dto);

        verify(usuarioRepository).save(usuario);
        verify(adminService, never()).criarRegistro(any(), any());
    }

    @Test
    void criarAdminCriaRegistroAdminNaMesmaOperacao() {
        UsuarioRequestDTO dto = dto(RoleUsuario.ADMIN, "COORDENADOR");
        Usuario usuario = usuario(RoleUsuario.ADMIN);
        when(usuarioMapper.toEntity(dto)).thenReturn(usuario);
        when(passwordEncoder.encode("01012000")).thenReturn("hash");

        usuarioService.criar(dto);

        verify(usuarioRepository).save(usuario);
        verify(adminService).criarRegistro(usuario, "COORDENADOR");
        assertTrue(usuario.getTrocarSenhaNoLogin());
    }

    @Test
    void criarAdminSemCargoFalhaAntesDePersistirUsuario() {
        UsuarioRequestDTO dto = dto(RoleUsuario.ADMIN, " ");

        assertThrows(ResourceConflictException.class, () -> usuarioService.criar(dto));

        verify(usuarioRepository, never()).save(any());
        verify(adminService, never()).criarRegistro(any(), any());
    }

    @Test
    void falhaAoCriarRegistroAdminInterrompeACriacaoTransacional() {
        UsuarioRequestDTO dto = dto(RoleUsuario.ADMIN, "COORDENADOR");
        Usuario usuario = usuario(RoleUsuario.ADMIN);
        when(usuarioMapper.toEntity(dto)).thenReturn(usuario);
        when(passwordEncoder.encode("01012000")).thenReturn("hash");
        doThrow(new IllegalStateException("falha simulada no registro Admin"))
                .when(adminService).criarRegistro(usuario, "COORDENADOR");

        assertThrows(IllegalStateException.class, () -> usuarioService.criar(dto));

        // O metodo testado e @Transactional; ao propagar a falha, o Spring desfaz inclusive o save do Usuario no banco.
        verify(usuarioRepository).save(usuario);
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

    @Test
    void criarAlunoSemSenhaLancaResourceConflictAntesDePersistir() {
        // Diferente do ADMIN (senha temporaria gerada pelo servidor), para ALUNO a senha e obrigatoria aqui.
        UsuarioRequestDTO dto = dto(RoleUsuario.ALUNO, null, "   ");
        Usuario usuario = usuario(RoleUsuario.ALUNO);
        when(usuarioMapper.toEntity(dto)).thenReturn(usuario);

        assertThrows(ResourceConflictException.class, () -> usuarioService.criar(dto));

        verify(usuarioRepository, never()).save(any());
    }

    private UsuarioRequestDTO dto(RoleUsuario role, String cargo) {
        return dto(role, cargo, "senha-segura");
    }

    private UsuarioRequestDTO dto(RoleUsuario role, String cargo, String senha) {
        return new UsuarioRequestDTO("Nome", senha, "RA001", LocalDate.of(2000, 1, 1), role, cargo, null, "email@teste.com");
    }

    private Usuario usuario(RoleUsuario role) {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setRa("RA001");
        usuario.setRole(role);
        return usuario;
    }
}