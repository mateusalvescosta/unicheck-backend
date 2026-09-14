package com.unicheck.checklist.domain;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "tb_checklist_prerequisito")
@NoArgsConstructor
public class ChecklistPrerequisito {
    @EmbeddedId
    private ChecklistPrerequisitoPK id = new ChecklistPrerequisitoPK();

    public ChecklistPrerequisito(Checklist checklist, Checklist requisito) {
        id.setChecklist(checklist);
        id.setChecklistRequisito(requisito);
    }

    public Checklist getChecklist() { return id.getChecklist(); }
    public Checklist getChecklistRequisito() { return id.getChecklistRequisito(); }
}
