package com.unicheck.beneficio.domain;

import java.time.OffsetDateTime;

import com.unicheck.usuario.domain.Aluno;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;

/**
 * @apiNote Marcação de "Meus favoritos": liga um aluno a um benefício, mapeada na
 *          tabela "tb_beneficio_favorito".
 *
 *          Guarda a data para que a lista de favoritos possa ser devolvida do mais
 *          recente para o mais antigo.
 * @author Mateus Alves Costa
 * @since 18.08.2026
 * @updated 19.09.2026
 */
@Entity
@Table(name = "tb_beneficio_favorito")
@NoArgsConstructor
public class BeneficioFavorito {

    @EmbeddedId
    private BeneficioFavoritoPK id = new BeneficioFavoritoPK();

    @Column(name = "favoritado_em", nullable = false, columnDefinition = "TIMESTAMP")
    private OffsetDateTime favoritadoEm;

    /**
     * @apiNote Cria a marcação de favorito já com a data do momento.
     * @param aluno     --> Aluno dono do favorito.
     * @param beneficio --> Benefício favoritado.
     * @author Mateus Alves Costa
     * @since 18.08.2026
     */
    public BeneficioFavorito(Aluno aluno, Beneficio beneficio) {
        id.setAluno(aluno);
        id.setBeneficio(beneficio);
        this.favoritadoEm = OffsetDateTime.now();
    }

    /**
     * @apiNote Rede de segurança para a data: garante o preenchimento mesmo quando
     *          a instância não foi criada pelo construtor de dois argumentos.
     * @author Mateus Alves Costa
     * @since 18.08.2026
     */
    @PrePersist
    protected void onCreate() {
        if (this.favoritadoEm == null) {
            this.favoritadoEm = OffsetDateTime.now();
        }
    }

    public Aluno getAluno() {
        return id.getAluno();
    }

    public void setAluno(Aluno aluno) {
        id.setAluno(aluno);
    }

    public Beneficio getBeneficio() {
        return id.getBeneficio();
    }

    public void setBeneficio(Beneficio beneficio) {
        id.setBeneficio(beneficio);
    }

    public OffsetDateTime getFavoritadoEm() {
        return favoritadoEm;
    }

    public void setFavoritadoEm(OffsetDateTime favoritadoEm) {
        this.favoritadoEm = favoritadoEm;
    }
}
