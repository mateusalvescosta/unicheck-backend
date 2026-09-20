package com.unicheck.beneficio.repository;

import java.util.Collection;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.unicheck.beneficio.domain.Beneficio;
import com.unicheck.beneficio.domain.BeneficioFavorito;
import com.unicheck.beneficio.domain.BeneficioFavoritoPK;

/**
 * @apiNote Repository que lida com o acesso a dados da entidade
 *          "BeneficioFavorito", a ligação entre um aluno e um benefício.
 * @author Mateus Alves Costa
 * @since 18.08.2026
 * @updated 19.09.2026
 */
public interface BeneficioFavoritoRepository extends JpaRepository<BeneficioFavorito, BeneficioFavoritoPK> {

    /**
     * @apiNote Descobre, entre os benefícios de uma página, quais o aluno favoritou.
     *          Uma consulta só para a página inteira, em vez de uma por benefício.
     * @param alunoId      --> ID do aluno dono dos favoritos.
     * @param beneficioIds --> IDs dos benefícios a verificar; não pode ser vazio.
     * @return --> Retorna os IDs dos benefícios que o aluno favoritou.
     * @author Mateus Alves Costa
     * @since 18.08.2026
     */
    @Query("""
            SELECT f.id.beneficio.id
            FROM BeneficioFavorito f
            WHERE f.id.aluno.id = :alunoId
              AND f.id.beneficio.id IN :beneficioIds
            """)
    Set<Long> findBeneficioIdsFavoritados(@Param("alunoId") Long alunoId,
                                          @Param("beneficioIds") Collection<Long> beneficioIds);

    /**
     * @apiNote Lista paginada dos benefícios favoritados por um aluno, para a tela
     *          "Meus favoritos".
     * @param alunoId  --> ID do aluno dono dos favoritos.
     * @param pageable --> Configuração de paginação, sem ordenação: a ordem é fixa
     *                 na consulta, do favorito mais recente para o mais antigo.
     * @return --> Retorna uma página com os benefícios favoritados.
     * @author Mateus Alves Costa
     * @since 18.08.2026
     */
    @Query("""
            SELECT f.id.beneficio
            FROM BeneficioFavorito f
            WHERE f.id.aluno.id = :alunoId
            ORDER BY f.favoritadoEm DESC
            """)
    Page<Beneficio> findBeneficiosPorAluno(@Param("alunoId") Long alunoId, Pageable pageable);

    /**
     * @apiNote Conta se o par aluno e benefício já está favoritado. Serve para
     *          tornar o favoritar idempotente sem depender da constraint do banco.
     * @param alunoId     --> ID do aluno.
     * @param beneficioId --> ID do benefício.
     * @return --> Retorna 1 caso já esteja favoritado, 0 caso contrário.
     * @author Mateus Alves Costa
     * @since 18.08.2026
     */
    @Query("""
            SELECT COUNT(f)
            FROM BeneficioFavorito f
            WHERE f.id.aluno.id = :alunoId AND f.id.beneficio.id = :beneficioId
            """)
    long contarFavorito(@Param("alunoId") Long alunoId, @Param("beneficioId") Long beneficioId);

    /**
     * @apiNote Remove o favorito do par aluno e benefício. Devolve a quantidade de
     *          linhas afetadas para que a Service distinga "removido" de "não
     *          estava favoritado" sem precisar consultar antes.
     * @param alunoId     --> ID do aluno.
     * @param beneficioId --> ID do benefício.
     * @return --> Retorna a quantidade de linhas removidas: 1 ou 0.
     * @author Mateus Alves Costa
     * @since 18.08.2026
     */
    @Modifying
    @Query("""
            DELETE FROM BeneficioFavorito f
            WHERE f.id.aluno.id = :alunoId AND f.id.beneficio.id = :beneficioId
            """)
    int deleteByAlunoAndBeneficio(@Param("alunoId") Long alunoId,
                                  @Param("beneficioId") Long beneficioId);
}
