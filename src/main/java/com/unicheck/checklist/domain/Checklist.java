package com.unicheck.checklist.domain;

import java.util.ArrayList;
import java.util.List;
import com.unicheck.checklist.enumeration.StatusChecklist;
import jakarta.persistence.*;
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

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, length = 500)
    private String descricao;

    @Column(nullable = false)
    private Integer ordem;

    /** Bônus concedido somente ao terminar a última etapa. */
    @Column(nullable = false)
    private Integer xp;

    @Column(name = "horas_complementares", nullable = false)
    private Integer horasComplementares;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusChecklist status;

    @OrderBy("ordem ASC")
    @OneToMany(mappedBy = "checklist", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemChecklist> itens = new ArrayList<>();
}
