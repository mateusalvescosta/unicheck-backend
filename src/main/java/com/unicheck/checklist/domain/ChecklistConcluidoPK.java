package com.unicheck.checklist.domain;

import com.unicheck.usuario.domain.Aluno;

import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@Embeddable
@NoArgsConstructor
public class ChecklistConcluidoPK {
    
    @ManyToOne
    @JoinColumn(name = "aluno_id")
    private Aluno aluno;

    @ManyToOne
    @JoinColumn(name = "checklist_id")
    private Checklist checklist;

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((aluno == null) ? 0 : aluno.hashCode());
        result = prime * result + ((checklist == null) ? 0 : checklist.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ChecklistConcluidoPK other = (ChecklistConcluidoPK) obj;
        if (aluno == null) {
            if (other.aluno != null)
                return false;
        } else if (!aluno.equals(other.aluno))
            return false;
        if (checklist == null) {
            if (other.checklist != null)
                return false;
        } else if (!checklist.equals(other.checklist))
            return false;
        return true;
    }
}
