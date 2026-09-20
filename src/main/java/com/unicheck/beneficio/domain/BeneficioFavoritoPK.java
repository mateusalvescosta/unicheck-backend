package com.unicheck.beneficio.domain;

import com.unicheck.usuario.domain.Aluno;

import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @apiNote Chave composta de BeneficioFavorito: o par aluno e benefício. É um
 *          Embeddable com dois ManyToOne, de modo que o próprio par já seja a
 *          identidade e o banco impeça o mesmo favorito duas vezes.
 * @author Mateus Alves Costa
 * @since 18.08.2026
 * @updated 19.09.2026
 */
@Setter
@Getter
@Embeddable
@NoArgsConstructor
public class BeneficioFavoritoPK {

    @ManyToOne
    @JoinColumn(name = "aluno_id")
    private Aluno aluno;

    @ManyToOne
    @JoinColumn(name = "beneficio_id")
    private Beneficio beneficio;

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((aluno == null) ? 0 : aluno.hashCode());
        result = prime * result + ((beneficio == null) ? 0 : beneficio.hashCode());
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
        BeneficioFavoritoPK other = (BeneficioFavoritoPK) obj;
        if (aluno == null) {
            if (other.aluno != null)
                return false;
        } else if (!aluno.equals(other.aluno))
            return false;
        if (beneficio == null) {
            if (other.beneficio != null)
                return false;
        } else if (!beneficio.equals(other.beneficio))
            return false;
        return true;
    }
}
