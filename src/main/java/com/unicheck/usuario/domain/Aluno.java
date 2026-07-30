package com.unicheck.usuario.domain;

import com.unicheck.curso.domain.Curso;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "tb_aluno")
@AllArgsConstructor
@NoArgsConstructor
public class Aluno {
   
    @Id
    @GeneratedValue(strategy  = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @Column(name = "curso_id", nullable = false)
    private Curso curso;

    @Column(nullable = false, unique = true, length = 25)
    private String ra;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_matricula",nullable = false, length = 25)
    private StatusMatricula status;

    @Column(nullable = false, length = 25)
    private String periodo;
}
