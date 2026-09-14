package com.unicheck.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SecurityConfigTest {
    @Test
    void rejeitaSegredoHs256Curto() {
        assertThrows(IllegalArgumentException.class, () -> new SecurityConfig("segredo-curto", "unicheck"));
    }

    @Test
    void aceitaSegredoHs256ComPeloMenosTrintaEDoisBytes() {
        assertDoesNotThrow(() -> new SecurityConfig("0123456789abcdef0123456789abcdef", "unicheck"));
    }

    @Test
    void decoderAceitaTokenValidoDoEmissorConfigurado() {
        SecurityConfig config = new SecurityConfig("0123456789abcdef0123456789abcdef", "https://unicheck.local");
        String token = config.jwtEncoder().encode(parametros(claims(
                "https://unicheck.local", Instant.now().minusSeconds(1), Instant.now().plusSeconds(60))))
                .getTokenValue();

        assertDoesNotThrow(() -> config.jwtDecoder().decode(token));
    }

    @Test
    void decoderRejeitaTokenExpiradoOuDeOutroEmissor() {
        SecurityConfig config = new SecurityConfig("0123456789abcdef0123456789abcdef", "https://unicheck.local");
        String expirado = config.jwtEncoder().encode(parametros(claims(
                "https://unicheck.local", Instant.now().minusSeconds(120), Instant.now().minusSeconds(60))))
                .getTokenValue();
        String outroEmissor = config.jwtEncoder().encode(parametros(claims(
                "https://outro-emissor.local", Instant.now().minusSeconds(1), Instant.now().plusSeconds(60))))
                .getTokenValue();

        assertThrows(JwtValidationException.class, () -> config.jwtDecoder().decode(expirado));
        assertThrows(JwtValidationException.class, () -> config.jwtDecoder().decode(outroEmissor));
    }

    private JwtClaimsSet claims(String issuer, Instant issuedAt, Instant expiresAt) {
        return JwtClaimsSet.builder().issuer(issuer).subject("RA001").issuedAt(issuedAt).expiresAt(expiresAt).build();
    }

    private JwtEncoderParameters parametros(JwtClaimsSet claims) {
        return JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims);
    }
}
