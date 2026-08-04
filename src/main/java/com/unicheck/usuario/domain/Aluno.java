package com.unicheck.usuario.domain;

import java.util.HashSet;
import java.util.Set;

import com.unicheck.checklist.domain.ChecklistConcluido;
import com.unicheck.checklist.domain.ProgressoItem;
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

    @Column(name = "xp_total", nullable = false)
    private Integer xpTotal;

    @PrePersist
    protected void onCreate() {
        if (this.xpTotal == null) {
            this.xpTotal = 0;
        }
    }

    @OneToMany(mappedBy = "aluno")
    private Set<ProgressoItem> progressos = new HashSet<>();

    @OneToMany(mappedBy = "aluno")
    private Set<ChecklistConcluido> concluidos = new HashSet<>()
}
