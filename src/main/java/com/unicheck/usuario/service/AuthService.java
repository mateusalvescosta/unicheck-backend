package com.unicheck.usuario.service;

import com.unicheck.usuario.domain.Usuario;
import com.unicheck.usuario.dto.LoginRequestDTO;
import com.unicheck.usuario.dto.LoginResponseDTO;
import com.unicheck.usuario.enumeration.RoleUsuario;
import com.unicheck.usuario.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtEncoder jwtEncoder;
    private final AdminRepository adminRepository;

    public LoginResponseDTO login(LoginRequestDTO dto) {
        // Delegar ao AuthenticationManager garante o mesmo fluxo de credencial para todo tipo de usuario.
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(dto.ra(), dto.senha()));
        Usuario usuario = (Usuario) authentication.getPrincipal();
        // O token contem apenas dados necessarios para autorizar requests sem consultar o banco a cada chamada.
        Instant agora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuedAt(agora).expiresAt(agora.plus(8, ChronoUnit.HOURS))
                .subject(usuario.getRa()).claim("role", usuario.getRole().name()).claim("userId", usuario.getId()).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
        // Cargo nao pertence a Usuario; consulta-se Admin somente quando a role realmente precisa dessa informacao.
        String cargo = usuario.getRole() == RoleUsuario.ADMIN
                ? adminRepository.findByUsuarioId(usuario.getId()).map(a -> a.getCargo()).orElse(null) : null;
        return new LoginResponseDTO(token, "Bearer", usuario.getRole(), cargo, usuario.getTrocarSenhaNoLogin());
    }
}
