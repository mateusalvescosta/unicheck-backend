package com.unicheck.beneficio.domain;

import java.util.HashSet;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "tb_beneficio")
public class Beneficio {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany
    @JoinColumn(name = "curso_id")
    private Curso curso;

    @Column(name = "icone_url", length = 255)
    private String iconeUrl;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, length = 255)
    private String descricao;

    @Column(nullable = false, length = 255)
    private String url;

    @ManyToMany
    @JoinTable(name = "tb_baneficio_categoria",
        joinColumns = @JoinColumn(name = "beneficio_id"),
        inverseJoinColumns = @JoinColumn(name = "categoria_id"))
    private Set<Categoria> categorias = new HashSet<>();
}
