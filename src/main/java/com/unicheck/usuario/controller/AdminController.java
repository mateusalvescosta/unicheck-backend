package com.unicheck.usuario.controller;

import com.unicheck.usuario.dto.AdminResponseDTO;
import com.unicheck.usuario.dto.CriarAdminRequestDTO;
import com.unicheck.usuario.service.AdminService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/admins")
@RequiredArgsConstructor
// A protecao na classe evita esquecer a restricao ao adicionar uma operacao administrativa nova.
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {
    private final AdminService adminService;

    @PostMapping
    public ResponseEntity<AdminResponseDTO> criar(@Valid @RequestBody CriarAdminRequestDTO dto) {
        // Somente um Admin autenticado pode iniciar este fluxo, resolvendo o caso de uso sem autocadastro de Admin.
        AdminResponseDTO response = adminService.criar(dto);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(uri).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminResponseDTO> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.buscarPorId(id));
    }

    @GetMapping
    public ResponseEntity<List<AdminResponseDTO>> listar() {
        return ResponseEntity.ok(adminService.listar());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        adminService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
