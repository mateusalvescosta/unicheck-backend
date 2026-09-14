package com.unicheck.checklist.domain;

import com.unicheck.usuario.domain.Aluno;

import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

@Setter
@Getter
@Embeddable
@NoArgsConstructor
public class ProgressoItemPK implements Serializable {

    private static final long serialVersionUID = 1L;
    
    @ManyToOne
    @JoinColumn(name = "aluno_id", nullable = false)
    private Aluno aluno;

    @ManyToOne
    @JoinColumn(name = "item_checklist_id", nullable = false)
    private ItemChecklist item;

    @Override
    public int hashCode() {
        return Objects.hash(idAluno(), idItem());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof ProgressoItemPK other))
            return false;
        return Objects.equals(idAluno(), other.idAluno()) && Objects.equals(idItem(), other.idItem());
    }

    private Long idAluno() { return aluno == null ? null : aluno.getId(); }
    private Long idItem() { return item == null ? null : item.getId(); }
}
