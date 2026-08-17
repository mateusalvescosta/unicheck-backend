package com.unicheck.curso.service;

import com.unicheck.curso.domain.Curso;
import com.unicheck.curso.dto.CursoRequestDTO;
import com.unicheck.curso.dto.CursoResponseDTO;
import com.unicheck.curso.mapper.CursoMapper;
import com.unicheck.curso.repository.CursoRepository;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CursoService {

    private final CursoRepository repository;
    private final CursoMapper mapper;

    public CursoService(CursoRepository repository, CursoMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public Page<CursoResponseDTO> findAll(String nome, Pageable pageable) {
        Page<Curso> result = repository.findByNomeContainingIgnoreCase(nome, pageable);
        return result.map(x -> mapper.toDto(x));
    }

    @Transactional
    public CursoResponseDTO create(CursoRequestDTO dto) {
        if(repository.existsByNomeIgnoreCase(dto.nome())){
            throw new DataIntegrityViolationException("Operação não permitida: violação de integridade dos dados.");
        }
        Curso curso = mapper.toEntity(dto);
        curso = repository.save(curso);
        return mapper.toDto(curso);
    }

    @Transactional
    public CursoResponseDTO update(Long id, CursoRequestDTO dto) {
        Curso curso = repository.getReferenceById(id);
        mapper.update(curso, dto);
        return mapper.toDto(curso);
    }

    @Transactional
    public void delete(Long id) {
        if(!repository.existsById(id)){
            throw new EntityNotFoundException("Recurso não encontrado.");
        }
        repository.deleteById(id);
    }
}
