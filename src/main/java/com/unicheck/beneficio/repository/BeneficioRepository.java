package com.unicheck.beneficio.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.unicheck.beneficio.domain.Beneficio;

public interface BeneficioRepository extends JpaRepository<Beneficio, Long> {

    Page<Beneficio> findByNomeContainingIgnoreCase(String nome, Pageable pageable);

    boolean existsByNomeIgnoreCase(String nome);
}
