package com.unicheck.checklist.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "tb_item_checklist")
@AllArgsConstructor
@NoArgsConstructor
public class ItemChecklist {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "checklist_id", nullable = false)
    private Checklist checklist;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(nullable = false, length = 500)
    private String descricao;

    /** Markdown armazenado como texto; a sanitização pertence ao frontend antes da renderização. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String conteudo;

    @Column(nullable = false)
    private Integer ordem;

    @Column(nullable = false)
    private Integer xp;
}
