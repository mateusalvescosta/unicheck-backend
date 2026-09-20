package com.unicheck.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;

/** Traduz falhas ocorridas antes do MVC sem expor detalhes de validacao do token. */
@Component
@RequiredArgsConstructor
public class SecurityProblemHandler implements AuthenticationEntryPoint, AccessDeniedHandler {
    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
            throws IOException {
        escrever(request, response, HttpStatus.UNAUTHORIZED, "Falha na autenticacao",
                "Autenticacao ausente ou invalida.");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       org.springframework.security.access.AccessDeniedException ex) throws IOException {
        escrever(request, response, HttpStatus.FORBIDDEN, "Acesso negado",
                "Nao foi possivel concluir esta operacao.");
    }

    void escrever(HttpServletRequest request, HttpServletResponse response, HttpStatus status,
                  String titulo, String detalhe) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detalhe);
        problem.setTitle(titulo);
        problem.setInstance(URI.create(request.getRequestURI()));
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
