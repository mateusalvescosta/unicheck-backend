package com.unicheck.manual.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @apiNote Passo numerado do bloco "Como fazer".
 *
 *          É um Embeddable porque um passo não existe fora do manual: é sempre
 *          lido, salvo e removido junto com ele.
 * @author Mateus Alves Costa
 * @since 27.09.2026
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
public class PassoManual {

    @Column(nullable = false)
    private Integer ordem;

    @Column(nullable = false, length = 255)
    private String descricao;
}
