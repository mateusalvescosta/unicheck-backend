package com.unicheck.checklist.controller;

import com.unicheck.checklist.dto.*;
import com.unicheck.checklist.service.ChecklistAlunoService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/checklists") @RequiredArgsConstructor
@PreAuthorize("hasRole('ALUNO')") @SecurityRequirement(name = "bearerAuth")
public class ChecklistAlunoController {
    private final ChecklistAlunoService service;
    @GetMapping public List<ChecklistAlunoResponseDTO> listar(@AuthenticationPrincipal Jwt jwt) { return service.listar(jwt.getSubject()); }
    @GetMapping("/{id}") public ChecklistAlunoResponseDTO buscar(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) { return service.buscar(id, jwt.getSubject()); }
    @PutMapping("/{id}/itens/{itemId}/conclusao") public ConclusaoEtapaResponseDTO concluir(@PathVariable Long id, @PathVariable Long itemId, @AuthenticationPrincipal Jwt jwt) { return service.concluir(id, itemId, jwt.getSubject()); }
}
