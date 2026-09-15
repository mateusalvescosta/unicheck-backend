package com.unicheck.gameficacao.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "tb_selo") @Getter @Setter @NoArgsConstructor
public class Selo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true, length = 100) private String nome;
    @Column(length = 500) private String descricao;
    @Column(name = "imagem_url", length = 500) private String imagemUrl;
}
