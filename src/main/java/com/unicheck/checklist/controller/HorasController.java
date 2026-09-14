package com.unicheck.checklist.controller;

import com.unicheck.checklist.dto.*;
import com.unicheck.checklist.enumeration.StatusLancamentoHoras;
import com.unicheck.checklist.service.HorasService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequiredArgsConstructor @SecurityRequirement(name = "bearerAuth")
public class HorasController {
    private final HorasService service;
    @GetMapping("/alunos/me/horas") @PreAuthorize("hasRole('ALUNO')")
    public ResumoHorasAlunoDTO minhas(@AuthenticationPrincipal Jwt jwt) { return service.minhasHoras(jwt.getSubject()); }
    @GetMapping("/admin/horas") @PreAuthorize("hasRole('ADMIN')")
    public List<HorasResponseDTO> listar(@RequestParam(required = false) StatusLancamentoHoras status) { return service.listar(status); }
    @PutMapping("/admin/horas/alunos/{alunoId}/checklists/{checklistId}/lancamento") @PreAuthorize("hasRole('ADMIN')")
    public HorasResponseDTO confirmar(@PathVariable Long alunoId, @PathVariable Long checklistId, @AuthenticationPrincipal Jwt jwt) { return service.confirmar(alunoId, checklistId, jwt.getSubject()); }
}
