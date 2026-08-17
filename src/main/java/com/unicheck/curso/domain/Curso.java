package com.unicheck.curso.domain;

import java.util.HashSet;
import java.util.Set;

import com.unicheck.beneficio.domain.Beneficio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "tb_curso")
@AllArgsConstructor
@NoArgsConstructor
public class Curso {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100, unique = true)
    private String nome;

    @ManyToMany(mappedBy = "cursos")
    private Set<Beneficio> beneficios = new HashSet<>();
}
