package com.unicheck.manual.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.unicheck.manual.domain.Manual;
import com.unicheck.manual.enumeration.CategoriaManual;

/**
 * @apiNote Repository que lida com o acesso a dados da entidade "Manual".
 * @author Mateus Alves Costa
 * @since 27.09.2026
 */
public interface ManualRepository extends JpaRepository<Manual, Long> {

    /**
     * @apiNote Busca de "Qual é a sua dúvida?" e listagem por assunto, em SQL
     *          nativo.
     *
     *          O CTE "filtro" existe só para dar tipo aos parâmetros uma única vez,
     *          como na busca de benefícios. O termo é procurado em todas as partes
     *          textuais do manual; os blocos em lista entram por EXISTS para que
     *          cada manual continue valendo uma linha só e a contagem da paginação
     *          feche com o conteúdo.
     *
     *          A ordenação é a da sequência dos manuais; o sort da URL não é
     *          repassado.
     * @param padrao    --> Termo já em minúsculas e entre %, ou null para não
     *                  filtrar por texto.
     * @param categoria --> Nome da constante de CategoriaManual, ou null para não
     *                  filtrar por categoria.
     * @param pageable  --> Configuração de paginação, sem ordenação.
     * @return --> Retorna uma página de manuais que atendem aos filtros.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Query(value = """
            WITH filtro AS (
                SELECT CAST(:padrao AS VARCHAR) AS padrao,
                       CAST(:categoria AS VARCHAR) AS categoria
            )
            SELECT m.* FROM tb_manual m, filtro f
            WHERE (f.categoria IS NULL OR m.categoria = f.categoria)
              AND (f.padrao IS NULL
                   OR LOWER(m.titulo) LIKE f.padrao
                   OR LOWER(m.resumo) LIKE f.padrao
                   OR LOWER(m.link_texto) LIKE f.padrao
                   OR LOWER(m.quando_se_aplica) LIKE f.padrao
                   OR LOWER(m.o_que_saber) LIKE f.padrao
                   OR LOWER(m.resultado_esperado) LIKE f.padrao
                   OR LOWER(m.proximo_passo_descricao) LIKE f.padrao
                   OR LOWER(m.aviso_atualizacao) LIKE f.padrao
                   OR EXISTS (SELECT 1 FROM tb_manual_atencao x
                               WHERE x.manual_id = m.id AND LOWER(x.descricao) LIKE f.padrao)
                   OR EXISTS (SELECT 1 FROM tb_manual_requisito x
                               WHERE x.manual_id = m.id AND LOWER(x.descricao) LIKE f.padrao)
                   OR EXISTS (SELECT 1 FROM tb_manual_passo x
                               WHERE x.manual_id = m.id AND LOWER(x.descricao) LIKE f.padrao)
                   OR EXISTS (SELECT 1 FROM tb_manual_local x
                               WHERE x.manual_id = m.id AND LOWER(x.nome) LIKE f.padrao)
                   OR EXISTS (SELECT 1 FROM tb_manual_prazo_regra x
                               WHERE x.manual_id = m.id AND LOWER(x.descricao) LIKE f.padrao)
                   OR EXISTS (SELECT 1 FROM tb_manual_detalhe x
                               WHERE x.manual_id = m.id AND LOWER(x.descricao) LIKE f.padrao)
                   OR EXISTS (SELECT 1 FROM tb_manual_entenda_melhor x
                               WHERE x.manual_id = m.id AND LOWER(x.descricao) LIKE f.padrao))
            ORDER BY m.ordem
            """,
            countQuery = """
            WITH filtro AS (
                SELECT CAST(:padrao AS VARCHAR) AS padrao,
                       CAST(:categoria AS VARCHAR) AS categoria
            )
            SELECT COUNT(*) FROM tb_manual m, filtro f
            WHERE (f.categoria IS NULL OR m.categoria = f.categoria)
              AND (f.padrao IS NULL
                   OR LOWER(m.titulo) LIKE f.padrao
                   OR LOWER(m.resumo) LIKE f.padrao
                   OR LOWER(m.link_texto) LIKE f.padrao
                   OR LOWER(m.quando_se_aplica) LIKE f.padrao
                   OR LOWER(m.o_que_saber) LIKE f.padrao
                   OR LOWER(m.resultado_esperado) LIKE f.padrao
                   OR LOWER(m.proximo_passo_descricao) LIKE f.padrao
                   OR LOWER(m.aviso_atualizacao) LIKE f.padrao
                   OR EXISTS (SELECT 1 FROM tb_manual_atencao x
                               WHERE x.manual_id = m.id AND LOWER(x.descricao) LIKE f.padrao)
                   OR EXISTS (SELECT 1 FROM tb_manual_requisito x
                               WHERE x.manual_id = m.id AND LOWER(x.descricao) LIKE f.padrao)
                   OR EXISTS (SELECT 1 FROM tb_manual_passo x
                               WHERE x.manual_id = m.id AND LOWER(x.descricao) LIKE f.padrao)
                   OR EXISTS (SELECT 1 FROM tb_manual_local x
                               WHERE x.manual_id = m.id AND LOWER(x.nome) LIKE f.padrao)
                   OR EXISTS (SELECT 1 FROM tb_manual_prazo_regra x
                               WHERE x.manual_id = m.id AND LOWER(x.descricao) LIKE f.padrao)
                   OR EXISTS (SELECT 1 FROM tb_manual_detalhe x
                               WHERE x.manual_id = m.id AND LOWER(x.descricao) LIKE f.padrao)
                   OR EXISTS (SELECT 1 FROM tb_manual_entenda_melhor x
                               WHERE x.manual_id = m.id AND LOWER(x.descricao) LIKE f.padrao))
            """,
            nativeQuery = true)
    Page<Manual> buscar(@Param("padrao") String padrao,
                        @Param("categoria") String categoria,
                        Pageable pageable);

    /**
     * @apiNote Conta os manuais de uma categoria ("9 orientações").
     * @param categoria --> Categoria a contar.
     * @return --> Retorna a quantidade de manuais da categoria.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    long countByCategoria(CategoriaManual categoria);

    /**
     * @apiNote Manual imediatamente anterior na sequência ("Conteúdo anterior").
     * @param ordem --> Ordem do manual de referência.
     * @return --> Retorna o anterior, ou vazio se o manual for o primeiro.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    Optional<Manual> findFirstByOrdemLessThanOrderByOrdemDesc(Integer ordem);

    /**
     * @apiNote Manual imediatamente seguinte na sequência ("Próximo conteúdo").
     * @param ordem --> Ordem do manual de referência.
     * @return --> Retorna o próximo, ou vazio se o manual for o último.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    Optional<Manual> findFirstByOrdemGreaterThanOrderByOrdemAsc(Integer ordem);

    /**
     * @apiNote Maior ordem em uso, para posicionar um manual no fim da sequência.
     * @return --> Retorna a maior ordem, ou zero quando não há manuais.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Query("SELECT COALESCE(MAX(m.ordem), 0) FROM Manual m")
    int maiorOrdem();

    /**
     * @apiNote Soma "delta" à ordem de todos os manuais no intervalo informado,
     *          abrindo ou fechando espaço na sequência.
     *
     *          No meio do UPDATE duas linhas chegam a ter a mesma ordem, por isso
     *          a constraint única de tb_manual.ordem é deferida para o commit. O
     *          contexto de persistência não é limpo: quem chama continua com a
     *          entidade que está movendo, cuja linha nunca está no intervalo.
     * @param de    --> Primeira ordem do intervalo, inclusiva.
     * @param ate   --> Última ordem do intervalo, inclusiva.
     * @param delta --> +1 para abrir espaço, -1 para fechar.
     * @return --> Retorna a quantidade de manuais deslocados.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE Manual m SET m.ordem = m.ordem + :delta WHERE m.ordem BETWEEN :de AND :ate")
    int deslocarOrdem(@Param("de") int de, @Param("ate") int ate, @Param("delta") int delta);

    /**
     * @apiNote Manuais que listam o manual informado em "Você também pode
     *          precisar". Usado antes da remoção, para tirá-lo dessas listas pelo
     *          Hibernate: se o ON DELETE CASCADE do banco apagasse a linha, a
     *          coluna de ordem ficaria com buraco e a lista viria com null.
     * @param id --> ID do manual referenciado.
     * @return --> Retorna os manuais que o referenciam.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Query("SELECT m FROM Manual m JOIN m.relacionados r WHERE r.id = :id")
    List<Manual> findQueRelacionam(@Param("id") Long id);

    /**
     * @apiNote Manuais que indicam o manual informado em "Seu próximo passo". Usado
     *          antes da remoção: o ON DELETE SET NULL do banco resolveria a coluna,
     *          mas um desses manuais já carregado na sessão continuaria apontando
     *          para o removido, e o Hibernate recusaria o flush.
     * @param id --> ID do manual indicado.
     * @return --> Retorna os manuais que o indicam.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    List<Manual> findByProximoPassoManualId(Long id);

    /**
     * @apiNote Verifica se já existe manual com o título informado, ignorando
     *          maiúsculas e minúsculas.
     * @param titulo --> Título a ser verificado.
     * @return --> Retorna true caso o título já esteja em uso.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    boolean existsByTituloIgnoreCase(String titulo);

    /**
     * @apiNote Mesma verificação de título duplicado, porém desconsiderando o
     *          próprio manual. Usado na atualização.
     * @param titulo --> Título a ser verificado.
     * @param id     --> ID do manual que deve ser ignorado na verificação.
     * @return --> Retorna true caso outro manual já use o título.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    boolean existsByTituloIgnoreCaseAndIdNot(String titulo, Long id);
}
