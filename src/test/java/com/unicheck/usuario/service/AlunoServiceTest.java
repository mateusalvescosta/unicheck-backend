package com.unicheck.usuario.service;

import com.unicheck.exception.ResourceConflictException;
import com.unicheck.exception.ResourceNotFoundException;
import com.unicheck.usuario.domain.Aluno;
import com.unicheck.usuario.domain.Usuario;
import com.unicheck.usuario.dto.AlunoRequestDTO;
import com.unicheck.usuario.dto.CadastroAlunoRequestDTO;
import com.unicheck.usuario.enumeration.RoleUsuario;
import com.unicheck.usuario.enumeration.StatusMatricula;
import com.unicheck.usuario.mapper.AlunoMapper;
import com.unicheck.usuario.repository.AlunoRepository;
import com.unicheck.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlunoServiceTest {
    @Mock
    private AlunoRepository alunoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private AlunoMapper alunoMapper;
    @Mock
    private UsuarioService usuarioService;
    @Mock
    private UsuarioValidacaoService usuarioValidacaoService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private AlunoService alunoService;

    @Test
    void criarComUsuarioAlunoFunciona() {
        AlunoRequestDTO dto = dto();
        Usuario usuario = usuario(RoleUsuario.ALUNO);
        Aluno aluno = new Aluno();
        when(alunoMapper.toEntity(dto)).thenReturn(aluno);
        when(usuarioService.buscarEntidade(1L)).thenReturn(usuario);
        when(alunoRepository.findByUsuarioId(1L)).thenReturn(Optional.empty());

        alunoService.criar(dto);

        verify(alunoRepository).save(aluno);
    }

    @Test
    void criarComUsuarioAdminERejeitado() {
        AlunoRequestDTO dto = dto();
        when(alunoMapper.toEntity(dto)).thenReturn(new Aluno());
        when(usuarioService.buscarEntidade(1L)).thenReturn(usuario(RoleUsuario.ADMIN));

        assertThrows(ResourceConflictException.class, () -> alunoService.criar(dto));

        verify(alunoRepository, never()).save(any());
    }

    @Test
    void criarComUsuarioInexistentePropagaNotFound() {
        AlunoRequestDTO dto = dto();
        when(alunoMapper.toEntity(dto)).thenReturn(new Aluno());
        when(usuarioService.buscarEntidade(1L)).thenThrow(new ResourceNotFoundException("Usuario", 1L));

        assertThrows(ResourceNotFoundException.class, () -> alunoService.criar(dto));
        verify(alunoRepository, never()).save(any());
    }

    @Test
    void cadastrarComDadosValidosCriaUsuarioAlunoComSenhaPropriaESemTrocaObrigatoria() {
        CadastroAlunoRequestDTO dto = cadastroDto();
        when(passwordEncoder.encode("senha123")).thenReturn("hash-senha123");

        alunoService.cadastrar(dto);

        // A validacao de RA/email duplicado precisa acontecer antes de qualquer save.
        verify(usuarioValidacaoService).validarUnicidade("RA001", "fulano@teste.com");

        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(usuarioCaptor.capture());
        Usuario usuarioSalvo = usuarioCaptor.getValue();
        assertEquals(RoleUsuario.ALUNO, usuarioSalvo.getRole());
        // Senha e a que o proprio aluno escolheu (hasheada), nunca senha temporaria pela data de nascimento.
        assertEquals("hash-senha123", usuarioSalvo.getSenha());
        assertFalse(usuarioSalvo.getTrocarSenhaNoLogin());

        ArgumentCaptor<Aluno> alunoCaptor = ArgumentCaptor.forClass(Aluno.class);
        verify(alunoRepository).save(alunoCaptor.capture());
        Aluno alunoSalvo = alunoCaptor.getValue();
        assertEquals(StatusMatricula.ATIVA, alunoSalvo.getStatus());
        assertEquals("1", alunoSalvo.getPeriodo());
    }

    @Test
    void cadastrarComRaOuEmailDuplicadoNaoSalvaUsuarioNemAluno() {
        CadastroAlunoRequestDTO dto = cadastroDto();
        doThrow(new ResourceConflictException("RA ja cadastrado"))
                .when(usuarioValidacaoService).validarUnicidade("RA001", "fulano@teste.com");

        assertThrows(ResourceConflictException.class, () -> alunoService.cadastrar(dto));

        verify(usuarioRepository, never()).save(any());
        verify(alunoRepository, never()).save(any());
    }

    private CadastroAlunoRequestDTO cadastroDto() {
        return new CadastroAlunoRequestDTO(
                "Fulano de Tal", "fulano@teste.com", "RA001",
                LocalDate.of(2005, 3, 15), "senha123", "1");
    }

    private AlunoRequestDTO dto() {
        return new AlunoRequestDTO(1L, StatusMatricula.ATIVA, "1");
    }

    private Usuario usuario(RoleUsuario role) {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setRole(role);
        return usuario;
    }
}