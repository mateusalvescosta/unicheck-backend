package com.unicheck.usuario.controller;

import com.unicheck.usuario.dto.*;
import com.unicheck.usuario.service.AlunoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class AlunoController {
    private final AlunoService alunoService;

    @PostMapping("/auth/cadastro")
    public ResponseEntity<AlunoResponseDTO> cadastrar(@Valid @RequestBody CadastroAlunoRequestDTO dto) {
        // Este e o unico endpoint de aluno sem token, pois e a porta de entrada do calouro.
        return criado(alunoService.cadastrar(dto));
    }

    @PostMapping("/alunos")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AlunoResponseDTO> criar(@Valid @RequestBody AlunoRequestDTO dto) {
        // Vincular um Usuario existente e operacao administrativa; por isso nao reutiliza o auto-cadastro.
        return criado(alunoService.criar(dto));
    }

    @GetMapping("/alunos/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AlunoResponseDTO> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(alunoService.buscarPorId(id));
    }

    @GetMapping("/alunos")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AlunoResponseDTO>> listar() {
        return ResponseEntity.ok(alunoService.listar());
    }

    @DeleteMapping("/alunos/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        alunoService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<AlunoResponseDTO> criado(AlunoResponseDTO response) {
        // Created inclui Location para permitir que o cliente descubra o recurso que acabou de nascer.
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(uri).body(response);
    }
}
