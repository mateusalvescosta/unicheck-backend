package com.unicheck.usuario.service;

import com.unicheck.exception.ResourceNotFoundException;
import com.unicheck.usuario.domain.Admin;
import com.unicheck.usuario.domain.Usuario;
import com.unicheck.usuario.dto.AdminResponseDTO;
import com.unicheck.usuario.dto.CriarAdminRequestDTO;
import com.unicheck.usuario.enumeration.RoleUsuario;
import com.unicheck.usuario.mapper.AdminMapper;
import com.unicheck.usuario.repository.AdminRepository;
import com.unicheck.usuario.repository.UsuarioRepository;
import com.unicheck.usuario.util.SenhaTemporariaUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final AdminRepository adminRepository;
    private final UsuarioRepository usuarioRepository;
    private final AdminMapper adminMapper;
    private final UsuarioValidacaoService usuarioValidacaoService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AdminResponseDTO criar(CriarAdminRequestDTO dto) {
        // Usuario e Admin sao duas tabelas por composicao: a transacao impede salvar somente metade do cadastro.
        usuarioValidacaoService.validarUnicidade(dto.ra(), dto.email());
        Usuario usuario = new Usuario();
        usuario.setNome(dto.nome());
        usuario.setEmail(dto.email());
        usuario.setRa(dto.ra());
        usuario.setDataNascimento(dto.dataNascimento());
        usuario.setRole(RoleUsuario.ADMIN);
        // A senha temporaria e derivada no servidor para o cliente nunca escolher credencial de primeiro acesso.
        usuario.setSenha(passwordEncoder.encode(SenhaTemporariaUtil.gerar(dto.dataNascimento())));
        usuario.setTrocarSenhaNoLogin(true);
        usuarioRepository.save(usuario);
        return adminMapper.toResponseDTO(criarRegistro(usuario, dto.cargo()));
    }

    /**
     * Mantem a gravacao do lado Admin em um unico lugar. UsuarioService reutiliza
     * este metodo para que o CRUD generico nao consiga produzir ADMIN sem tb_admin.
     */
    Admin criarRegistro(Usuario usuario, String cargo) {
        Admin admin = new Admin();
        admin.setUsuario(usuario);
        admin.setCargo(cargo);
        return adminRepository.save(admin);
    }

    @Transactional(readOnly = true)
    public AdminResponseDTO buscarPorId(Long id) {
        return adminMapper.toResponseDTO(buscarEntidade(id));
    }

    @Transactional(readOnly = true)
    public List<AdminResponseDTO> listar() {
        return adminRepository.findAll().stream().map(adminMapper::toResponseDTO).toList();
    }

    @Transactional
    public void deletar(Long id) {
        Admin admin = buscarEntidade(id);
        Usuario usuario = admin.getUsuario();
        // Papel e perfil formam uma unica identidade: remover apenas tb_admin deixaria um ADMIN incoerente.
        adminRepository.delete(admin);
        adminRepository.flush();
        usuarioRepository.delete(usuario);
    }

    private Admin buscarEntidade(Long id) {
        return adminRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Admin", id));
    }
}
