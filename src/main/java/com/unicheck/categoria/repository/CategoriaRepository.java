package com.unicheck.categoria.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.unicheck.categoria.domain.Categoria;

/**
 * @apiNote Repository que lida com o acesso a dados da entidade "Categoria".
 * @author Mateus Alves Costa
 * @since 18.08.2026
 * @updated 19.09.2026
 */
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    /**
     * @apiNote Lista todas as categorias, ativas e desativadas, em ordem alfabética.
     * @return --> Retorna a lista completa de categorias ordenada por nome.
     * @author Mateus Alves Costa
     * @since 18.08.2026
     */
    List<Categoria> findAllByOrderByNomeAsc();

    /**
     * @apiNote Lista apenas as categorias ativas, em ordem alfabética. É esta a
     *          lista que pode ser oferecida no cadastro de um benefício.
     * @return --> Retorna as categorias ativas ordenadas por nome.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    List<Categoria> findAllByAtivoTrueOrderByNomeAsc();

    /**
     * @apiNote Verifica se já existe categoria com o nome informado, ignorando
     *          maiúsculas e minúsculas.
     * @param nome --> Nome a ser verificado.
     * @return --> Retorna true caso o nome já esteja em uso.
     * @author Mateus Alves Costa
     * @since 18.08.2026
     */
    boolean existsByNomeIgnoreCase(String nome);

    /**
     * @apiNote Mesma verificação de nome duplicado, porém desconsiderando a própria
     *          categoria. Usado na atualização, onde manter o nome atual é válido.
     * @param nome --> Nome a ser verificado.
     * @param id   --> ID da categoria que deve ser ignorada na verificação.
     * @return --> Retorna true caso outra categoria já use o nome.
     * @author Mateus Alves Costa
     * @since 18.08.2026
     */
    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);
}
