package com.unicheck.categoria.domain;

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

/**
 * @apiNote Entidade que representa uma categoria de benefício, mapeada na tabela
 *          "tb_categoria". O lado dono do relacionamento com benefício é
 *          Beneficio, por isso aqui a coleção é apenas mapeada por ele.
 * @author Mateus Alves Costa
 * @since 30.07.2026
 * @updated 19.09.2026
 */
@Setter
@Getter
@Entity
@Table(name = "tb_categoria")
@AllArgsConstructor
@NoArgsConstructor
public class Categoria {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    /**
     * @apiNote Categoria desativada some da lista oferecida no cadastro de
     *          benefício, mas continua valendo nos benefícios que já a usam. Por
     *          isso é uma flag, e não um delete.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Column(nullable = false)
    private boolean ativo = true;

    @ManyToMany(mappedBy = "categorias")
    private Set<Beneficio> beneficios = new HashSet<>();
}
