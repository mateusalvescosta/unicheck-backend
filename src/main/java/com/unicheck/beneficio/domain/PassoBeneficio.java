package com.unicheck.beneficio.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @apiNote Passo do bloco "Como conseguir" exibido quando o card é expandido.
 *
 *          É um Embeddable porque um passo não existe fora do benefício: é sempre
 *          lido, salvo e removido junto com ele.
 * @author Mateus Alves Costa
 * @since 18.08.2026
 * @updated 19.09.2026
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
public class PassoBeneficio {

    @Column(nullable = false)
    private Integer ordem;

    @Column(nullable = false, length = 100)
    private String titulo;

    @Column(nullable = false, length = 255)
    private String descricao;
}
