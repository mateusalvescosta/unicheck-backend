package com.unicheck.usuario.service;

import com.unicheck.usuario.domain.Admin;
import com.unicheck.usuario.domain.Usuario;
import com.unicheck.usuario.dto.LoginRequestDTO;
import com.unicheck.usuario.dto.LoginResponseDTO;
import com.unicheck.usuario.enumeration.RoleUsuario;
import com.unicheck.usuario.repository.AdminRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtEncoder jwtEncoder;
    @Mock private AdminRepository adminRepository;
    @InjectMocks private AuthService authService;

    @Test
    void loginComCredenciaisValidasDeAlunoRetornaTokenSemCargo() {
        Usuario usuario = usuario(RoleUsuario.ALUNO, "RA001", false);
        mockAutenticacaoComSucesso(usuario);
        when(jwtEncoder.encode(any())).thenReturn(jwtFake("token-aluno"));

        LoginResponseDTO response = authService.login(new LoginRequestDTO("RA001", "senha123"));

        assertEquals("token-aluno", response.token());
        assertEquals("Bearer", response.tipo());
        assertEquals(RoleUsuario.ALUNO, response.role());
        assertNull(response.cargo());
        assertFalse(response.trocarSenhaObrigatorio());
        // Aluno nao tem cargo; a Service nem deve consultar o AdminRepository nesse caso.
        verify(adminRepository, never()).findByUsuarioId(any());
    }

    @Test
    void loginComCredenciaisValidasDeAdminRetornaCargoEFlagDeTrocaDeSenha() {
        Usuario usuario = usuario(RoleUsuario.ADMIN, "ADMIN0001", true);
        mockAutenticacaoComSucesso(usuario);
        when(jwtEncoder.encode(any())).thenReturn(jwtFake("token-admin"));
        Admin admin = new Admin();
        admin.setUsuario(usuario);
        admin.setCargo("COORDENADOR");
        when(adminRepository.findByUsuarioId(usuario.getId())).thenReturn(Optional.of(admin));

        LoginResponseDTO response = authService.login(new LoginRequestDTO("ADMIN0001", "01011990"));

        assertEquals("token-admin", response.token());
        assertEquals(RoleUsuario.ADMIN, response.role());
        assertEquals("COORDENADOR", response.cargo());
        // trocarSenhaNoLogin=true simula o primeiro acesso apos criacao pelo fluxo de Admin.
        assertTrue(response.trocarSenhaObrigatorio());
    }

    @Test
    void loginComRaOuSenhaInvalidosPropagaBadCredentialsSemGerarToken() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("RA ou senha incorretos"));

        assertThrows(BadCredentialsException.class,
                () -> authService.login(new LoginRequestDTO("RA001", "senha-errada")));

        // Nao pode chegar a emitir token se a autenticacao falhou.
        verify(jwtEncoder, never()).encode(any());
        verify(adminRepository, never()).findByUsuarioId(any());
    }

    private void mockAutenticacaoComSucesso(Usuario usuario) {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(usuario);
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
    }

    private Usuario usuario(RoleUsuario role, String ra, boolean trocarSenhaNoLogin) {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setRa(ra);
        usuario.setRole(role);
        usuario.setTrocarSenhaNoLogin(trocarSenhaNoLogin);
        return usuario;
    }

    private Jwt jwtFake(String tokenValue) {
        return Jwt.withTokenValue(tokenValue)
                .header("alg", "HS256")
                .claim("sub", "RA001")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }
}