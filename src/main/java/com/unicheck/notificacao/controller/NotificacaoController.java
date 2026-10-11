package com.unicheck.notificacao.controller;

import com.unicheck.notificacao.dto.AlterarPreferenciaRequestDTO;
import com.unicheck.notificacao.dto.NotificacaoResponseDTO;
import com.unicheck.notificacao.dto.PreferenciaNotificacaoDTO;
import com.unicheck.notificacao.dto.TotalNaoLidasResponseDTO;
import com.unicheck.notificacao.enumeration.TipoNotificacao;
import com.unicheck.notificacao.service.NotificacaoAlunoService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Notificações do aluno autenticado; o RA sempre vem do token, nunca da requisição. */
@RestController
@RequestMapping("/notificacoes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ALUNO')")
@SecurityRequirement(name = "bearerAuth")
public class NotificacaoController {

    private final NotificacaoAlunoService service;

    /** Mais recentes primeiro; sem o parâmetro lida, traz lidas e não lidas. */
    @GetMapping
    public ResponseEntity<Page<NotificacaoResponseDTO>> listar(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Boolean lida,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.listar(jwt.getSubject(), lida, PageRequest.of(page, size)));
    }

    @GetMapping("/nao-lidas/total")
    public ResponseEntity<TotalNaoLidasResponseDTO> totalNaoLidas(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(service.totalNaoLidas(jwt.getSubject()));
    }

    @PatchMapping("/{id}/lida")
    public ResponseEntity<Void> marcarComoLida(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        service.marcarComoLida(id, jwt.getSubject());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/lidas")
    public ResponseEntity<Void> marcarTodasComoLidas(@AuthenticationPrincipal Jwt jwt) {
        service.marcarTodasComoLidas(jwt.getSubject());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/preferencias")
    public ResponseEntity<List<PreferenciaNotificacaoDTO>> listarPreferencias(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(service.listarPreferencias(jwt.getSubject()));
    }

    /** Tipo fora do enum na URL cai no 400 de parâmetro inválido do GlobalExceptionHandler. */
    @PutMapping("/preferencias/{tipo}")
    public ResponseEntity<Void> alterarPreferencia(@PathVariable TipoNotificacao tipo,
            @Valid @RequestBody AlterarPreferenciaRequestDTO dto, @AuthenticationPrincipal Jwt jwt) {
        service.alterarPreferencia(jwt.getSubject(), tipo, dto.ativo());
        return ResponseEntity.noContent().build();
    }
}
