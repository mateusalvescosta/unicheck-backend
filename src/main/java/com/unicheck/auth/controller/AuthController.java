package com.unicheck.auth.controller;

import com.unicheck.auth.dto.LoginRequestDTO;
import com.unicheck.auth.dto.LoginResponseDTO;
import com.unicheck.auth.service.AuthService;
import com.unicheck.usuario.dto.AlunoResponseDTO;
import com.unicheck.usuario.dto.CadastroAlunoRequestDTO;
import com.unicheck.usuario.service.AlunoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final AlunoService alunoService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto) {
        // O token so e emitido depois de a Service delegar a verificacao ao AuthenticationManager.
        return ResponseEntity.ok(authService.login(dto));
    }

    @PostMapping("/cadastro")
    public ResponseEntity<AlunoResponseDTO> cadastrar(@Valid @RequestBody CadastroAlunoRequestDTO dto) {
        AlunoResponseDTO response = alunoService.cadastrar(dto);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(uri).body(response);
    }
}
