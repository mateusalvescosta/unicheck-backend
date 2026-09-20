package com.unicheck.checklist.domain;

import java.time.OffsetDateTime;

import com.unicheck.checklist.enumeration.StatusProgressoItem;
import com.unicheck.usuario.domain.Aluno;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tb_progresso_item")
@NoArgsConstructor
public class ProgressoItem {
    
    @EmbeddedId
    private ProgressoItemPK id = new ProgressoItemPK();
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusProgressoItem status;

    // A migration permite nulo porque um item PENDENTE ainda nao possui instante de conclusao.
    @Column(name = "data_conclusao", columnDefinition = "TIMESTAMP")
    private OffsetDateTime dataConclusao;

    /** Valor histórico: nunca é recalculado a partir do template da etapa. */
    @Column(name = "xp_creditado")
    private Integer xpCreditado;

    public ProgressoItem(Aluno aluno, ItemChecklist itemChecklist, StatusProgressoItem status, OffsetDateTime dataConclusao) {
        id.setAluno(aluno);
        id.setItem(itemChecklist);
        this.status = status;
        this.dataConclusao = dataConclusao;
    }

    public Aluno getAluno() {
        return id.getAluno();
    }

    public void setItem(ItemChecklist itemChecklist) {
        id.setItem(itemChecklist);
    }

    public ItemChecklist getItem() {
        return id.getItem();
    }

    public void setAluno(Aluno aluno) {
        id.setAluno(aluno);
    }

    public StatusProgressoItem getStatus() {
        return status;
    }

    public void setStatus(StatusProgressoItem status) {
        this.status = status;
    }

    public OffsetDateTime getDataConclusao() {
        return dataConclusao;
    }

    public void setDataConclusao(OffsetDateTime dataConclusao) {
        this.dataConclusao = dataConclusao;
    }

    public Integer getXpCreditado() { return xpCreditado; }
    public void setXpCreditado(Integer xpCreditado) { this.xpCreditado = xpCreditado; }
}
