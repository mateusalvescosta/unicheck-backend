package com.unicheck.usuario.domain;

import com.unicheck.curso.domain.Curso;

import com.unicheck.usuario.enumeration.StatusMatricula;
import jakarta.persistence.*;
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

    @OneToOne
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @ManyToOne
    @JoinColumn(name = "curso_id", nullable = false)
    private Curso curso;

    @Column(nullable = false, unique = true, length = 25)
    private String ra;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_matricula",nullable = false, length = 25)
    private StatusMatricula status;

    @Column(nullable = false, length = 25)
    private String periodo;
}
