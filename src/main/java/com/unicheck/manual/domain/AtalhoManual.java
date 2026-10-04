package com.unicheck.manual.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @apiNote Item de "Atalhos do dia a dia", mapeado na tabela
 *          "tb_manual_atalho". Tem nome próprio ("Emitir boleto"), que não
 *          coincide com o título do manual para onde aponta.
 *
 *          A lista é escolhida pelo admin como um todo, por isso não tem CRUD
 *          item a item: é sempre substituída inteira.
 * @author Mateus Alves Costa
 * @since 27.09.2026
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "tb_manual_atalho")
public class AtalhoManual {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "manual_id", nullable = false, unique = true)
    private Manual manual;

    @Column(nullable = false, length = 60)
    private String nome;

    @Column(nullable = false, unique = true)
    private Integer ordem;
}
