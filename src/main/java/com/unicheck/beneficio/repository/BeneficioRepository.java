package com.unicheck.beneficio.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.unicheck.beneficio.domain.Beneficio;

/**
 * @apiNote Repository que lida com o acesso a dados da entidade "Beneficio".
 * @author Mateus Alves Costa
 * @since 18.08.2026
 * @updated 19.09.2026
 */
public interface BeneficioRepository extends JpaRepository<Beneficio, Long> {

    /**
     * @apiNote Busca da listagem, em SQL nativo.
     *
     *          O CTE "filtro" existe so para dar tipo aos parametros uma unica vez.
     *          O CAST e necessario porque o Postgres nao infere o tipo de um
     *          parametro que aparece comparado com NULL, e sem o CTE ele teria de
     *          ser repetido em cada uso. O CTE devolve exatamente uma linha, entao o
     *          cruzamento com tb_beneficio nao multiplica nada.
     *
     *          Categoria e palavra-chave entram por EXISTS em vez de JOIN para que
     *          um beneficio com varias categorias continue valendo uma linha so: sem
     *          linha repetida nao e preciso DISTINCT e a contagem da paginacao fecha
     *          com o conteudo.
     *
     *          A ordenacao e fixa aqui dentro; o sort da URL nao e repassado.
     * @param padrao      --> Termo de busca ja em minusculas e entre %, ou null para
     *                    nao filtrar por texto.
     * @param tipo        --> Nome da constante de TipoBeneficio, ou null para nao
     *                    filtrar por tipo.
     * @param categoriaId --> ID da categoria, ou null para nao filtrar por categoria.
     * @param pageable    --> Configuracao de paginacao, sem ordenacao.
     * @return --> Retorna uma pagina de beneficios que atendem aos filtros.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Query(value = """
            WITH filtro AS (
                SELECT CAST(:padrao AS VARCHAR) AS padrao,
                       CAST(:tipo AS VARCHAR) AS tipo,
                       CAST(:categoriaId AS BIGINT) AS categoria_id
            )
            SELECT b.* FROM tb_beneficio b, filtro f
            WHERE (f.tipo IS NULL OR b.tipo = f.tipo)
              AND (f.categoria_id IS NULL OR EXISTS (
                    SELECT 1 FROM tb_beneficio_categoria bc
                     WHERE bc.beneficio_id = b.id
                       AND bc.categoria_id = f.categoria_id))
              AND (f.padrao IS NULL
                   OR LOWER(b.nome) LIKE f.padrao
                   OR LOWER(b.descricao) LIKE f.padrao
                   OR LOWER(b.chamada) LIKE f.padrao
                   OR LOWER(b.publico_elegivel) LIKE f.padrao
                   OR EXISTS (
                        SELECT 1 FROM tb_beneficio_categoria bc
                          JOIN tb_categoria c ON c.id = bc.categoria_id
                         WHERE bc.beneficio_id = b.id
                           AND LOWER(c.nome) LIKE f.padrao)
                   OR EXISTS (
                        SELECT 1 FROM tb_beneficio_palavra_chave pc
                         WHERE pc.beneficio_id = b.id
                           AND LOWER(pc.palavra_chave) LIKE f.padrao))
            ORDER BY b.nome
            """,
            countQuery = """
            WITH filtro AS (
                SELECT CAST(:padrao AS VARCHAR) AS padrao,
                       CAST(:tipo AS VARCHAR) AS tipo,
                       CAST(:categoriaId AS BIGINT) AS categoria_id
            )
            SELECT COUNT(*) FROM tb_beneficio b, filtro f
            WHERE (f.tipo IS NULL OR b.tipo = f.tipo)
              AND (f.categoria_id IS NULL OR EXISTS (
                    SELECT 1 FROM tb_beneficio_categoria bc
                     WHERE bc.beneficio_id = b.id
                       AND bc.categoria_id = f.categoria_id))
              AND (f.padrao IS NULL
                   OR LOWER(b.nome) LIKE f.padrao
                   OR LOWER(b.descricao) LIKE f.padrao
                   OR LOWER(b.chamada) LIKE f.padrao
                   OR LOWER(b.publico_elegivel) LIKE f.padrao
                   OR EXISTS (
                        SELECT 1 FROM tb_beneficio_categoria bc
                          JOIN tb_categoria c ON c.id = bc.categoria_id
                         WHERE bc.beneficio_id = b.id
                           AND LOWER(c.nome) LIKE f.padrao)
                   OR EXISTS (
                        SELECT 1 FROM tb_beneficio_palavra_chave pc
                         WHERE pc.beneficio_id = b.id
                           AND LOWER(pc.palavra_chave) LIKE f.padrao))
            """,
            nativeQuery = true)
    Page<Beneficio> buscar(@Param("padrao") String padrao,
                           @Param("tipo") String tipo,
                           @Param("categoriaId") Long categoriaId,
                           Pageable pageable);

    /**
     * @apiNote Busca o beneficio ja com categorias, palavras-chave e passos
     *          carregados, para o card expandido. O EntityGraph evita o N+1 que
     *          apareceria ao ler cada colecao depois.
     * @param id --> ID do beneficio a ser buscado.
     * @return --> Retorna o beneficio com os detalhes, ou vazio se nao existir.
     * @author Mateus Alves Costa
     * @since 18.08.2026
     */
    @EntityGraph(attributePaths = {"categorias", "palavrasChave", "passos"})
    Optional<Beneficio> findWithDetalhesById(Long id);

    /**
     * @apiNote Verifica se ja existe beneficio com o nome informado, ignorando
     *          maiusculas e minusculas.
     * @param nome --> Nome a ser verificado.
     * @return --> Retorna true caso o nome ja esteja em uso.
     * @author Mateus Alves Costa
     * @since 18.08.2026
     */
    boolean existsByNomeIgnoreCase(String nome);

    /**
     * @apiNote Mesma verificacao de nome duplicado, porem desconsiderando o proprio
     *          beneficio. Usado na atualizacao, onde manter o nome atual e valido.
     * @param nome --> Nome a ser verificado.
     * @param id   --> ID do beneficio que deve ser ignorado na verificacao.
     * @return --> Retorna true caso outro beneficio ja use o nome.
     * @author Mateus Alves Costa
     * @since 18.08.2026
     */
    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);
}
