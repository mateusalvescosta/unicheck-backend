package com.unicheck.beneficio.controller;

import java.net.URI;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.unicheck.beneficio.dto.BeneficioRequestDTO;
import com.unicheck.beneficio.dto.BeneficioResponseDTO;
import com.unicheck.beneficio.dto.BeneficioSummaryDTO;
import com.unicheck.beneficio.service.BeneficioService;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/beneficios")
public class BeneficioController {
    private final BeneficioService service;

    public BeneficioController(BeneficioService beneficioService) {
        this.service = beneficioService;
    }

    @PostMapping
    public ResponseEntity<BeneficioResponseDTO> create(@Valid @RequestBody BeneficioRequestDTO dto) {
        BeneficioResponseDTO beneficio = service.create(dto);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
            .buildAndExpand(beneficio.id()).toUri();
        return ResponseEntity.created(uri).body(beneficio);
    }

    @PostMapping("/{beneficioId}/categorias/{categoriaId}")
    public ResponseEntity<Void> addCategoria(@PathVariable Long beneficioId, @PathVariable Long categoriaId) {
        service.addCategoria(beneficioId, categoriaId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<Page<BeneficioSummaryDTO>> findByNome(@RequestParam(defaultValue = "") String nome, @PageableDefault(size = 20) Pageable pageable) {
        Page<BeneficioSummaryDTO> beneficios = service.findByNome(nome, pageable);
        return ResponseEntity.ok().body(beneficios);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BeneficioResponseDTO> findById(@PathVariable Long id) {
        BeneficioResponseDTO beneficio = service.findById(id);
        return ResponseEntity.ok().body(beneficio);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BeneficioResponseDTO> update(@PathVariable Long id, @Valid @RequestBody BeneficioRequestDTO dto) {
        BeneficioResponseDTO beneficio = service.update(id, dto);
        return ResponseEntity.ok().body(beneficio);
    }


    @DeleteMapping("/{beneficioId}/categorias/{categoriaId}")
    public ResponseEntity<Void> removeCategoria(@PathVariable Long beneficioId, @PathVariable Long categoriaId) {
        service.removeCategoria(beneficioId, categoriaId);
        return ResponseEntity.noContent().build();
    }
}
