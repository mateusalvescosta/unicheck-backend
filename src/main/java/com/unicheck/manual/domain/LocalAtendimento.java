package com.unicheck.manual.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @apiNote Local exibido como chip no bloco "Onde resolver". A url é opcional:
 *          há locais que são apenas indicados (uma secretaria, por exemplo) e
 *          não têm para onde apontar.
 * @author Mateus Alves Costa
 * @since 27.09.2026
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
public class LocalAtendimento {

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(length = 255)
    private String url;
}
