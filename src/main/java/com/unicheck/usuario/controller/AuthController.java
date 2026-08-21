package com.unicheck.usuario.controller;

import com.unicheck.usuario.dto.LoginRequestDTO;
import com.unicheck.usuario.dto.LoginResponseDTO;
import com.unicheck.usuario.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto) {
        // O token so e emitido depois de a Service delegar a verificacao ao AuthenticationManager.
        return ResponseEntity.ok(authService.login(dto));
    }
}
