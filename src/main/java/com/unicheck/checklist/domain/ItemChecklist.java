package com.unicheck.checklist.domain;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
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

    @OneToMany(mappedBy = "id.item")
    private Set<ProgressoItem> progressos = new HashSet<>();

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(nullable = false)
    private int ordem;
}
