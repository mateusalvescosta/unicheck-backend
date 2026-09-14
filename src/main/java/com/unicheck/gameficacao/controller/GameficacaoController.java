package com.unicheck.gameficacao.controller;

import com.unicheck.gameficacao.dto.GameficacaoResponseDTO;
import com.unicheck.gameficacao.service.GameficacaoService;
import com.unicheck.usuario.service.IdentidadeService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/alunos/me/gamificacao") @RequiredArgsConstructor
@PreAuthorize("hasRole('ALUNO')") @SecurityRequirement(name = "bearerAuth")
public class GameficacaoController {
    private final GameficacaoService service;
    private final IdentidadeService identidadeService;
    @GetMapping public GameficacaoResponseDTO consultar(@AuthenticationPrincipal Jwt jwt) { return service.consultar(identidadeService.alunoPorRa(jwt.getSubject())); }
}
