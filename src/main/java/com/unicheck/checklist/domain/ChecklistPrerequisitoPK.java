package com.unicheck.checklist.domain;

import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@Embeddable
@NoArgsConstructor
public class ChecklistPrerequisitoPK implements Serializable {
    @ManyToOne
    @JoinColumn(name = "checklist_id", nullable = false)
    private Checklist checklist;

    @ManyToOne
    @JoinColumn(name = "checklist_requisito_id", nullable = false)
    private Checklist checklistRequisito;

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChecklistPrerequisitoPK that)) return false;
        return Objects.equals(id(checklist), id(that.checklist))
                && Objects.equals(id(checklistRequisito), id(that.checklistRequisito));
    }
    @Override public int hashCode() { return Objects.hash(id(checklist), id(checklistRequisito)); }
    private Long id(Checklist valor) { return valor == null ? null : valor.getId(); }
}
