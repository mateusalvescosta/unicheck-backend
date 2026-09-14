package com.unicheck.checklist.domain;

import java.time.OffsetDateTime;

import com.unicheck.usuario.domain.Aluno;
import com.unicheck.usuario.domain.Admin;
import com.unicheck.checklist.enumeration.StatusLancamentoHoras;

import jakarta.persistence.*;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tb_checklist_concluido")
@NoArgsConstructor
public class ChecklistConcluido {

    @EmbeddedId
    private ChecklistConcluidoPK id = new ChecklistConcluidoPK();

    @Column(name = "data_conclusao", columnDefinition = "TIMESTAMP", nullable = false)
    private OffsetDateTime dataConclusao;

    @Column(name = "xp_creditado")
    private Integer xpCreditado;

    @Column(name = "horas_creditadas")
    private Integer horasCreditadas;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_lancamento_horas", length = 20)
    private StatusLancamentoHoras statusLancamentoHoras;

    @Column(name = "data_lancamento_horas")
    private OffsetDateTime dataLancamentoHoras;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_lancamento_id")
    private Admin adminLancamento;

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

    public Integer getXpCreditado() {
        return xpCreditado;
    }

    public void setXpCreditado(Integer xpCreditado) {
        this.xpCreditado = xpCreditado;
    }

    public StatusLancamentoHoras getStatusLancamentoHoras() { return statusLancamentoHoras; }
    public void setStatusLancamentoHoras(StatusLancamentoHoras status) { this.statusLancamentoHoras = status; }
    public OffsetDateTime getDataLancamentoHoras() { return dataLancamentoHoras; }
    public void setDataLancamentoHoras(OffsetDateTime data) { this.dataLancamentoHoras = data; }
    public Admin getAdminLancamento() { return adminLancamento; }
    public void setAdminLancamento(Admin admin) { this.adminLancamento = admin; }
}
