package com.unicheck.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** Impede usar uma credencial temporaria para acessar dados antes que ela seja substituida. */
@Component
@RequiredArgsConstructor
public class TrocaSenhaObrigatoriaFilter extends OncePerRequestFilter {
    private final SecurityProblemHandler problemHandler;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken token && exigeTroca(token.getToken())
                && !ehTrocaDaPropriaSenha(request)) {
            // O token continua criptograficamente valido, mas concede somente o caso de uso de troca de senha.
            problemHandler.escrever(request, response, HttpStatus.FORBIDDEN, "Troca de senha obrigatoria",
                    "Troque a senha temporaria antes de acessar outros recursos.");
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean exigeTroca(Jwt jwt) {
        return Boolean.TRUE.equals(jwt.getClaimAsBoolean("trocarSenhaObrigatorio"));
    }

    private boolean ehTrocaDaPropriaSenha(HttpServletRequest request) {
        return HttpMethod.PATCH.matches(request.getMethod())
                && "/usuarios/trocar-senha".equals(request.getRequestURI());
    }
}
