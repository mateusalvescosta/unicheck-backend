package com.unicheck.gameficacao.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "tb_nivel") @Getter @Setter @NoArgsConstructor
public class Nivel {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true, length = 100) private String nome;
    @Column(name = "xp_minimo", nullable = false, unique = true) private Integer xpMinimo;
    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "selo_id", nullable = false, unique = true) private Selo selo;
}
