package com.unicheck.usuario.service;

import com.unicheck.exception.ResourceNotFoundException;
import com.unicheck.exception.ResourceConflictException;
import com.unicheck.usuario.domain.Aluno;
import com.unicheck.usuario.domain.Usuario;
import com.unicheck.usuario.dto.AlunoRequestDTO;
import com.unicheck.usuario.dto.AlunoResponseDTO;
import com.unicheck.usuario.dto.CadastroAlunoRequestDTO;
import com.unicheck.usuario.enumeration.RoleUsuario;
import com.unicheck.usuario.enumeration.StatusMatricula;
import com.unicheck.usuario.mapper.AlunoMapper;
import com.unicheck.usuario.repository.AlunoRepository;
import com.unicheck.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AlunoService {
    private final AlunoRepository alunoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AlunoMapper alunoMapper;
    private final UsuarioService usuarioService;
    private final UsuarioValidacaoService usuarioValidacaoService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Cadastro publico sempre cria ALUNO; o cliente nunca escolhe a role.
     */
    @Transactional
    public AlunoResponseDTO cadastrar(CadastroAlunoRequestDTO dto) {
        // As duas gravacoes precisam falhar juntas para nao deixar Usuario sem Aluno em caso de curso invalido.
        usuarioValidacaoService.validarUnicidade(dto.ra(), dto.email());
        Usuario usuario = new Usuario();
        usuario.setNome(dto.nome());
        usuario.setEmail(dto.email());
        usuario.setRa(dto.ra());
        usuario.setDataNascimento(dto.dataNascimento());
        usuario.setRole(RoleUsuario.ALUNO);
        // O proprio aluno escolhe a senha no cadastro; nao ha mais senha temporaria/troca obrigatoria para ALUNO.
        usuario.setSenha(passwordEncoder.encode(passwordEncoder.encode(dto.senha())));
        usuario.setTrocarSenhaNoLogin(false);
        usuarioRepository.save(usuario);

        Aluno aluno = new Aluno();
        aluno.setUsuario(usuario);
        // O auto-cadastro representa uma matricula valida no MVP; status nao e escolhido pelo cliente.
        aluno.setStatus(StatusMatricula.ATIVA);
        aluno.setPeriodo(dto.periodo());
        return alunoMapper.toResponseDTO(alunoRepository.save(aluno));
    }

    @Transactional
    public AlunoResponseDTO criar(AlunoRequestDTO dto) {
        // O Mapper nao busca relacionamentos: IDs devem ser resolvidos pela Service para garantir que existam.
        Aluno aluno = alunoMapper.toEntity(dto);
        Usuario usuario = usuarioService.buscarEntidade(dto.usuarioId());
        if (usuario.getRole() != RoleUsuario.ALUNO) {
            // A composicao representa o papel real do usuario; Admin nao pode ser matriculado como Aluno.
            throw new ResourceConflictException("Usuario informado nao possui role ALUNO");
        }
        if (alunoRepository.findByUsuarioId(usuario.getId()).isPresent()) {
            // A relacao e um-para-um; antecipar o conflito evita depender de erro tecnico de constraint unica.
            throw new ResourceConflictException("Usuario ja possui cadastro de aluno");
        }
        aluno.setUsuario(usuario);
        return alunoMapper.toResponseDTO(alunoRepository.save(aluno));
    }

    @Transactional(readOnly = true)
    public AlunoResponseDTO buscarPorId(Long id) {
        return alunoMapper.toResponseDTO(buscarEntidade(id));
    }

    @Transactional(readOnly = true)
    public List<AlunoResponseDTO> listar() {
        return alunoRepository.findAll().stream().map(alunoMapper::toResponseDTO).toList();
    }

    @Transactional
    public void deletar(Long id) {
        // Busca antes de excluir para devolver 404 consistente em vez de um delete silencioso.
        alunoRepository.delete(buscarEntidade(id));
    }

    private Aluno buscarEntidade(Long id) {
        return alunoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Aluno", id));
    }

}
