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
public class ChecklistConcluidoPK implements Serializable {

    private static final long serialVersionUID = 1L;
    
    @ManyToOne
    @JoinColumn(name = "aluno_id", nullable = false)
    private Aluno aluno;

    @ManyToOne
    @JoinColumn(name = "checklist_id", nullable = false)
    private Checklist checklist;

    @Override
    public int hashCode() {
        return Objects.hash(idAluno(), idChecklist());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof ChecklistConcluidoPK other))
            return false;
        return Objects.equals(idAluno(), other.idAluno()) && Objects.equals(idChecklist(), other.idChecklist());
    }

    private Long idAluno() { return aluno == null ? null : aluno.getId(); }
    private Long idChecklist() { return checklist == null ? null : checklist.getId(); }
}
