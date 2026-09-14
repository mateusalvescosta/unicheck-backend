package com.unicheck.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;

import static org.mockito.Mockito.*;

class TrocaSenhaObrigatoriaFilterTest {
    private final SecurityProblemHandler problemHandler = mock(SecurityProblemHandler.class);
    private final TrocaSenhaObrigatoriaFilter filter = new TrocaSenhaObrigatoriaFilter(problemHandler);
    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock(HttpServletResponse.class);
    private final FilterChain chain = mock(FilterChain.class);

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void bloqueiaOutrosRecursosEnquantoTrocaForObrigatoria() throws Exception {
        autenticar(true);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/admins");

        filter.doFilterInternal(request, response, chain);

        verify(chain, never()).doFilter(any(), any());
        verify(problemHandler).escrever(request, response, HttpStatus.FORBIDDEN,
                "Troca de senha obrigatoria", "Troque a senha temporaria antes de acessar outros recursos.");
    }

    @Test
    void permiteTrocarAPropriaSenha() throws Exception {
        autenticar(true);
        when(request.getMethod()).thenReturn("PATCH");
        when(request.getRequestURI()).thenReturn("/usuarios/trocar-senha");

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(problemHandler);
    }

    @Test
    void permiteFluxoNormalQuandoTrocaNaoForObrigatoria() throws Exception {
        autenticar(false);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    private void autenticar(boolean trocaObrigatoria) {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "HS256").subject("RA001")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60))
                .claim("trocarSenhaObrigatorio", trocaObrigatoria).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
