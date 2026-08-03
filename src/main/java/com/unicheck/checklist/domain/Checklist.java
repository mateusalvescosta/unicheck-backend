package com.unicheck.checklist.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.unicheck.curso.domain.Curso;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "tb_checklist")
@AllArgsConstructor
@NoArgsConstructor
public class Checklist {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "curso_id")
    private Curso curso;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(columnDefinition = "TEXT", length = 500)
    private String descricao;

    @Column(name = "horas_complementares")
    private Integer horasComplementares;

    @OneToMany(mappedBy = "checklist")    
    List<ItemChecklist> itens = new ArrayList<>();

    @ManyToMany
    @JoinTable(name = "tb_checklist_prerequisito",
        joinColumns = @JoinColumn(name = "checklist_id"),
        inverseJoinColumns = @JoinColumn(name = "checklist_requisito_id"))
    Set<Checklist> prerequisitos = new HashSet<>();

    @OneToMany(mappedBy = "id.checklist")
    private Set<ChecklistConcluido> concluidos = new HashSet<>();
}
