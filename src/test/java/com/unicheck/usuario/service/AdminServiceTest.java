package com.unicheck.usuario.service;

import com.unicheck.usuario.domain.Admin;
import com.unicheck.usuario.domain.Usuario;
import com.unicheck.usuario.mapper.AdminMapper;
import com.unicheck.usuario.repository.AdminRepository;
import com.unicheck.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {
    @Mock private AdminRepository adminRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private AdminMapper adminMapper;
    @Mock private UsuarioValidacaoService usuarioValidacaoService;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private AdminService adminService;

    @Test
    void deletarRemovePerfilAntesDoUsuarioRelacionado() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        Admin admin = new Admin();
        admin.setId(10L);
        admin.setUsuario(usuario);
        when(adminRepository.findById(10L)).thenReturn(Optional.of(admin));

        adminService.deletar(10L);

        var ordem = inOrder(adminRepository, usuarioRepository);
        ordem.verify(adminRepository).delete(admin);
        ordem.verify(adminRepository).flush();
        ordem.verify(usuarioRepository).delete(usuario);
    }
}
