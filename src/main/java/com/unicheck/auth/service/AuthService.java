package com.unicheck.auth.service;

import com.unicheck.auth.dto.LoginRequestDTO;
import com.unicheck.auth.dto.LoginResponseDTO;
import com.unicheck.usuario.domain.Usuario;
import com.unicheck.usuario.enumeration.RoleUsuario;
import com.unicheck.usuario.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtEncoder jwtEncoder;
    private final AdminRepository adminRepository;
    @org.springframework.beans.factory.annotation.Value("${security.jwt.issuer:https://unicheck.local}")
    private String jwtIssuer = "https://unicheck.local";

    public LoginResponseDTO login(LoginRequestDTO dto) {
        // Delegar ao AuthenticationManager garante o mesmo fluxo de credencial para todo tipo de usuario.
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(dto.ra(), dto.senha()));
        Usuario usuario = (Usuario) authentication.getPrincipal();
        // Um papel ADMIN sem o perfil correspondente é uma identidade inconsistente e não pode receber autoridade administrativa.
        String cargo = usuario.getRole() == RoleUsuario.ADMIN
                ? adminRepository.findByUsuarioId(usuario.getId()).map(a -> a.getCargo())
                .orElseThrow(() -> new BadCredentialsException("Credenciais invalidas"))
                : null;
        // O token contem apenas dados necessarios para autorizar requests sem consultar o banco a cada chamada.
        Instant agora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer(jwtIssuer).issuedAt(agora)
                .expiresAt(agora.plus(8, ChronoUnit.HOURS)).subject(usuario.getRa())
                .claim("role", usuario.getRole().name()).claim("userId", usuario.getId())
                // A claim permite que a cadeia de seguranca limite o primeiro acesso antes do Controller.
                .claim("trocarSenhaObrigatorio", usuario.getTrocarSenhaNoLogin()).build();
        // Informar HS256 no cabecalho e necessario para o Nimbus selecionar a chave simetrica de assinatura.
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new LoginResponseDTO(token, "Bearer", usuario.getRole(), cargo, usuario.getTrocarSenhaNoLogin());
    }
}
