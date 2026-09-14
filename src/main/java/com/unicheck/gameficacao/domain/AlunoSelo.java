package com.unicheck.gameficacao.domain;

import com.unicheck.usuario.domain.Aluno;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity @Table(name = "tb_aluno_selo") @Getter @NoArgsConstructor
public class AlunoSelo {
    @EmbeddedId private AlunoSeloPK id = new AlunoSeloPK();
    @Column(name = "data_conquista", nullable = false) private OffsetDateTime dataConquista;
    public AlunoSelo(Aluno aluno, Selo selo, OffsetDateTime data) {
        id.setAluno(aluno); id.setSelo(selo); dataConquista = data;
    }
    public Selo getSelo() { return id.getSelo(); }
}
