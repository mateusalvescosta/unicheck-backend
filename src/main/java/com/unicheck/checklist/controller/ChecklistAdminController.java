package com.unicheck.checklist.controller;

import com.unicheck.checklist.dto.*;
import com.unicheck.checklist.service.ChecklistAdminService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/admin/checklists") @RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')") @SecurityRequirement(name = "bearerAuth")
public class ChecklistAdminController {
    private final ChecklistAdminService service;
    @PostMapping public ResponseEntity<ChecklistAdminResponseDTO> criar(@Valid @RequestBody ChecklistRequestDTO dto) { return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(dto)); }
    @GetMapping public List<ChecklistAdminResponseDTO> listar() { return service.listar(); }
    @GetMapping("/{id}") public ChecklistAdminResponseDTO buscar(@PathVariable Long id) { return service.buscar(id); }
    @PutMapping("/{id}") public ChecklistAdminResponseDTO atualizar(@PathVariable Long id, @Valid @RequestBody ChecklistRequestDTO dto) { return service.atualizar(id, dto); }
    @PostMapping("/{id}/itens") public ResponseEntity<ItemChecklistResponseDTO> adicionarItem(@PathVariable Long id, @Valid @RequestBody ItemChecklistRequestDTO dto) { return ResponseEntity.status(HttpStatus.CREATED).body(service.adicionarItem(id, dto)); }
    @PutMapping("/{id}/itens/{itemId}") public ItemChecklistResponseDTO atualizarItem(@PathVariable Long id, @PathVariable Long itemId, @Valid @RequestBody ItemChecklistRequestDTO dto) { return service.atualizarItem(id, itemId, dto); }
    @DeleteMapping("/{id}/itens/{itemId}") @ResponseStatus(HttpStatus.NO_CONTENT) public void removerItem(@PathVariable Long id, @PathVariable Long itemId) { service.removerItem(id, itemId); }
    @PostMapping("/{id}/prerequisitos/{requisitoId}") public ChecklistAdminResponseDTO adicionarPrerequisito(@PathVariable Long id, @PathVariable Long requisitoId) { return service.adicionarPrerequisito(id, requisitoId); }
    @DeleteMapping("/{id}/prerequisitos/{requisitoId}") @ResponseStatus(HttpStatus.NO_CONTENT) public void removerPrerequisito(@PathVariable Long id, @PathVariable Long requisitoId) { service.removerPrerequisito(id, requisitoId); }
    @PostMapping("/{id}/publicar") public ChecklistAdminResponseDTO publicar(@PathVariable Long id) { return service.publicar(id); }
    @PostMapping("/{id}/arquivar") public ChecklistAdminResponseDTO arquivar(@PathVariable Long id) { return service.arquivar(id); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void excluir(@PathVariable Long id) { service.excluir(id); }
}
