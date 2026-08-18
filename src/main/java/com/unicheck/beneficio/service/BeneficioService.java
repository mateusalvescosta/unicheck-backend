package com.unicheck.beneficio.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.unicheck.beneficio.domain.Beneficio;
import com.unicheck.beneficio.dto.BeneficioResponseDTO;
import com.unicheck.beneficio.dto.BeneficioSummaryDTO;
import com.unicheck.beneficio.dto.BeneficioRequestDTO;
import com.unicheck.beneficio.mapper.BeneficioMapper;
import com.unicheck.beneficio.repository.BeneficioRepository;
import com.unicheck.categoria.domain.Categoria;
import com.unicheck.categoria.repository.CategoriaRepository;

import jakarta.persistence.EntityNotFoundException;


@Service
public class BeneficioService {
    private final BeneficioRepository beneficioRepository;
    private final CategoriaRepository categoriaRepository;
    private final BeneficioMapper mapper;


    public BeneficioService(BeneficioRepository beneficioRepository, CategoriaRepository categoriaRepository, BeneficioMapper beneficioMapper) {
        this.beneficioRepository = beneficioRepository;
        this.categoriaRepository = categoriaRepository;
        this.mapper = beneficioMapper;
    }

    @Transactional
    public BeneficioResponseDTO create(BeneficioRequestDTO dto) {
        if(beneficioRepository.existsByNomeIgnoreCase(dto.nome())) {
            throw new DataIntegrityViolationException("Benefício com o mesmo nome já existe.");
        }
        Beneficio beneficio = mapper.toEntity(dto);
        beneficio = beneficioRepository.save(beneficio);
        return mapper.toDto(beneficio);
    }

    @Transactional
    public void addCategoria(Long beneficioId, Long categoriaId) {
        if(!beneficioRepository.existsById(beneficioId) || !categoriaRepository.existsById(categoriaId)) {
            throw new EntityNotFoundException("Benefício ou Categoria não encontrados.");
        }
        Beneficio beneficio = beneficioRepository.getReferenceById(beneficioId);
        Categoria categoria = categoriaRepository.getReferenceById(categoriaId);
        beneficio.getCategorias().add(categoria);
        beneficioRepository.save(beneficio);
    }

    @Transactional(readOnly = true)
    public Page<BeneficioSummaryDTO> findByNome(String nome, Pageable pageable) {
        Page<Beneficio> resultado = beneficioRepository.findByNomeContainingIgnoreCase(nome, pageable);
        return resultado.map(x -> mapper.toDtoSummary(x));
    }

    @Transactional(readOnly = true)
    public BeneficioResponseDTO findById(Long id) {
        Beneficio beneficio = beneficioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Benefício não encontrado."));
        return mapper.toDto(beneficio);
    }

    @Transactional
    public BeneficioResponseDTO update(Long id, BeneficioRequestDTO dto) {
        if(beneficioRepository.existsByNomeIgnoreCase(dto.nome())) {
            throw new DataIntegrityViolationException("Benefício com o mesmo nome já existe.");
        }
        
        Beneficio beneficio = beneficioRepository.getReferenceById(id);
        mapper.update(beneficio, dto);
        beneficio = beneficioRepository.save(beneficio);
        return mapper.toDto(beneficio);
    }

    @Transactional
    public void delete(Long id) {
        if(!beneficioRepository.existsById(id)) {
            throw new EntityNotFoundException("Benefício não encontrado.");
        }
        beneficioRepository.deleteById(id);
    }

    @Transactional
    public void removeCategoria(Long beneficioId, Long categoriaId) {
        if(!beneficioRepository.existsById(beneficioId) || !categoriaRepository.existsById(categoriaId)) {
            throw new EntityNotFoundException("Benefício ou Categoria não encontrados.");
        }
        Beneficio beneficio = beneficioRepository.getReferenceById(beneficioId);
        beneficio.getCategorias().removeIf(categoria -> categoria.getId().equals(categoriaId));
        beneficioRepository.save(beneficio);
    }

}
