package com.unicheck.categoria.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.unicheck.categoria.domain.Categoria;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    
}
