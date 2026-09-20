package com.unicheck.categoria.service;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.unicheck.categoria.domain.Categoria;
import com.unicheck.categoria.dto.CategoriaRequestDTO;
import com.unicheck.categoria.dto.CategoriaResponseDTO;
import com.unicheck.categoria.repository.CategoriaRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

/**
 * @apiNote Serviço que lida com a lógica de negócio da entidade "Categoria".
 *          Categoria não é removida no fluxo normal: ela é desativada, porque os
 *          benefícios já ligados a ela continuam valendo.
 * @author Mateus Alves Costa
 * @since 18.08.2026
 * @updated 19.09.2026
 */
@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository repository;

    /**
     * @apiNote Lista todas as categorias, ativas e desativadas, para a tela de
     *          administração.
     * @return --> Retorna a lista completa de categorias ordenada por nome.
     * @author Mateus Alves Costa
     * @since 18.08.2026
     */
    @Transactional(readOnly = true)
    public List<CategoriaResponseDTO> findAll() {
        return repository.findAllByOrderByNomeAsc().stream().map(this::toDto).toList();
    }

    /**
     * @apiNote Lista oferecida no cadastro de benefício: só o que pode ser
     *          escolhido hoje.
     * @return --> Retorna as categorias ativas ordenadas por nome.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Transactional(readOnly = true)
    public List<CategoriaResponseDTO> findAllAtivas() {
        return repository.findAllByAtivoTrueOrderByNomeAsc().stream().map(this::toDto).toList();
    }

    /**
     * @apiNote Busca uma categoria pelo seu ID, ativa ou não.
     * @param id --> ID da categoria a ser buscada.
     * @return --> Retorna os dados da categoria encontrada.
     * @throws EntityNotFoundException --> Lançada caso a categoria com o ID
     *                                   fornecido não seja encontrada.
     * @author Mateus Alves Costa
     * @since 18.08.2026
     */
    @Transactional(readOnly = true)
    public CategoriaResponseDTO findById(Long id) {
        Categoria categoria = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Categoria não encontrada com id: " + id));
        return toDto(categoria);
    }

    /**
     * @apiNote Cria uma nova categoria, já ativa.
     * @param dto --> Dados para criação da categoria.
     * @return --> Retorna os dados da categoria criada.
     * @throws DataIntegrityViolationException --> Lançada caso o nome fornecido já esteja
     *                                   cadastrado.
     * @author Mateus Alves Costa
     * @since 18.08.2026
     */
    @Transactional
    public CategoriaResponseDTO create(CategoriaRequestDTO dto) {
        if(repository.existsByNomeIgnoreCase(dto.nome())) {
            throw new DataIntegrityViolationException("Já existe uma categoria com o nome '" + dto.nome().trim() + "'.");
        }

        Categoria categoria = new Categoria();
        categoria.setNome(dto.nome().trim());
        categoria = repository.save(categoria);
        return toDto(categoria);
    }

    /**
     * @apiNote Atualiza o nome de uma categoria existente. O estado ativo não é
     *          alterado aqui: para isso existem ativar e desativar.
     * @param id  --> ID da categoria a ser atualizada.
     * @param dto --> Dados para atualização da categoria.
     * @return --> Retorna os dados da categoria atualizada.
     * @throws EntityNotFoundException --> Lançada caso a categoria com o ID
     *                                   fornecido não seja encontrada.
     * @throws DataIntegrityViolationException --> Lançada caso o nome fornecido já esteja
     *                                   cadastrado em outra categoria.
     * @author Mateus Alves Costa
     * @since 18.08.2026
     */
    @Transactional
    public CategoriaResponseDTO update(Long id, CategoriaRequestDTO dto) {
        Categoria categoria = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Categoria não encontrada com id: " + id));

        if(repository.existsByNomeIgnoreCaseAndIdNot(dto.nome(), id)) {
            throw new DataIntegrityViolationException("Já existe outra categoria com o nome '" + dto.nome().trim() + "'.");
        }

        categoria.setNome(dto.nome().trim());
        categoria = repository.save(categoria);
        return toDto(categoria);
    }

    /**
     * @apiNote Reativa uma categoria, devolvendo-a à lista oferecida no cadastro de
     *          benefício.
     * @param id --> ID da categoria a ser ativada.
     * @return --> Retorna os dados da categoria já ativa.
     * @throws EntityNotFoundException --> Lançada caso a categoria com o ID
     *                                   fornecido não seja encontrada.
     * @throws DataIntegrityViolationException --> Lançada caso a categoria já esteja
     *                                   ativa.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Transactional
    public CategoriaResponseDTO ativar(Long id) {
        Categoria categoria = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Categoria não encontrada com id: " + id));

        if(categoria.isAtivo()) {
            throw new DataIntegrityViolationException("A categoria '" + categoria.getNome() + "' já está ativa.");
        }

        categoria.setAtivo(true);
        categoria = repository.save(categoria);
        return toDto(categoria);
    }

    /**
     * @apiNote Desativa uma categoria. Ela sai da lista de escolha, mas os
     *          benefícios que já a usam não são tocados e continuam aparecendo
     *          normalmente na busca.
     * @param id --> ID da categoria a ser desativada.
     * @return --> Retorna os dados da categoria já desativada.
     * @throws EntityNotFoundException --> Lançada caso a categoria com o ID
     *                                   fornecido não seja encontrada.
     * @throws DataIntegrityViolationException --> Lançada caso a categoria já esteja
     *                                   desativada.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Transactional
    public CategoriaResponseDTO desativar(Long id) {
        Categoria categoria = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Categoria não encontrada com id: " + id));

        if(!categoria.isAtivo()) {
            throw new DataIntegrityViolationException("A categoria '" + categoria.getNome() + "' já está desativada.");
        }

        // Os beneficios que ja usam a categoria nao sao tocados: desativar so tira ela da
        // lista de escolha, nao desfaz o que ja foi cadastrado.
        categoria.setAtivo(false);
        categoria = repository.save(categoria);
        return toDto(categoria);
    }

    /**
     * @apiNote Remove a categoria em definitivo. No fluxo normal prefira desativar:
     *          se a categoria estiver vinculada a algum benefício, a constraint do
     *          banco impede a remoção.
     * @param id --> ID da categoria a ser removida.
     * @throws EntityNotFoundException        --> Lançada caso a categoria com o ID
     *                                          fornecido não seja encontrada.
     * @throws DataIntegrityViolationException --> Lançada caso a categoria ainda
     *                                          esteja vinculada a algum benefício.
     * @author Mateus Alves Costa
     * @since 18.08.2026
     */
    @Transactional
    public void delete(Long id) {
        if(!repository.existsById(id)) {
            throw new EntityNotFoundException("Categoria não encontrada com id: " + id);
        }
        repository.deleteById(id);
    }

    /**
     * @apiNote Converte uma entidade Categoria em um objeto CategoriaResponseDTO.
     * @param categoria --> Entidade Categoria a ser convertida.
     * @return --> Retorna os dados da categoria.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    private CategoriaResponseDTO toDto(Categoria categoria) {
        return new CategoriaResponseDTO(categoria.getId(), categoria.getNome(), categoria.isAtivo());
    }
}
