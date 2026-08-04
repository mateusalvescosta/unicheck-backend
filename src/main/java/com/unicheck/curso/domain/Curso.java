package com.unicheck.curso.domain;

import java.util.ArrayList;
import java.util.List;

import com.unicheck.checklist.domain.Checklist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
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

    @Column(nullable = false, length = 100)
    private String nome;

    @OneToMany(mappedBy = "curso")
    private List<Checklist> checklists = new ArrayList<>();
}
