package com.unicheck.checklist.domain;

import java.time.OffsetDateTime;

import com.unicheck.usuario.domain.Aluno;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tb_checklist_concluido")
@NoArgsConstructor
public class ChecklistConcluido {
    
    @EmbeddedId
    private ChecklistConcluidoPK id = new ChecklistConcluidoPK();

    @Column(columnDefinition = "TIMESTAMP")
    private OffsetDateTime dataConclusao;

    @Column(name = "xp_creditado")
    private Integer xpCreditado;

    @Column(name = "horas_creditadas")
    private Integer horasCreditadas;

    public ChecklistConcluido(Aluno aluno, Checklist checklist, Integer horasCreditadas, OffsetDateTime dataConclusao) {
        id.setAluno(aluno);
        id.setChecklist(checklist);
        this.dataConclusao = dataConclusao;
        this.horasCreditadas = horasCreditadas;
    }

    public Aluno getAluno() {
        return id.getAluno();
    }

    public void setAluno(Aluno aluno) {
        id.setAluno(aluno);
    }

    public Checklist getChecklist() {
        return id.getChecklist();
    }

    public void setChecklist(Checklist checklist) {
        id.setChecklist(checklist);
    }

    public OffsetDateTime getDataConclusao() {
        return dataConclusao;
    }

    public void setDataConclusao(OffsetDateTime dataConclusao) {
        this.dataConclusao = dataConclusao;
    }

    public Integer getHorasCreditadas() {
        return horasCreditadas;
    }

    public void setHorasCreditadas(Integer horasCreditadas) {
        this.horasCreditadas = horasCreditadas;
    }
}
