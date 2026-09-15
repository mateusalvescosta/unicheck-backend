package com.unicheck.gameficacao.domain;

import com.unicheck.usuario.domain.Aluno;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.io.Serializable;
import java.util.Objects;

@Embeddable @Getter @Setter @NoArgsConstructor
public class AlunoSeloPK implements Serializable {
    @ManyToOne @JoinColumn(name = "aluno_id", nullable = false) private Aluno aluno;
    @ManyToOne @JoinColumn(name = "selo_id", nullable = false) private Selo selo;
    @Override public boolean equals(Object o) {
        return this == o || o instanceof AlunoSeloPK that
                && Objects.equals(idAluno(), that.idAluno()) && Objects.equals(idSelo(), that.idSelo());
    }
    @Override public int hashCode() { return Objects.hash(idAluno(), idSelo()); }
    private Long idAluno() { return aluno == null ? null : aluno.getId(); }
    private Long idSelo() { return selo == null ? null : selo.getId(); }
}
