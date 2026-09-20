package com.unicheck.beneficio.service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.unicheck.beneficio.domain.Beneficio;
import com.unicheck.beneficio.domain.BeneficioFavorito;
import com.unicheck.beneficio.domain.PassoBeneficio;
import com.unicheck.beneficio.dto.BeneficioRequestDTO;
import com.unicheck.beneficio.dto.BeneficioResponseDTO;
import com.unicheck.beneficio.dto.BeneficioSummaryDTO;
import com.unicheck.beneficio.dto.PassoBeneficioDTO;
import com.unicheck.beneficio.enumeration.TipoBeneficio;
import com.unicheck.beneficio.repository.BeneficioFavoritoRepository;
import com.unicheck.beneficio.repository.BeneficioRepository;
import com.unicheck.categoria.domain.Categoria;
import com.unicheck.categoria.dto.CategoriaResponseDTO;
import com.unicheck.categoria.repository.CategoriaRepository;
import com.unicheck.exception.ResourceConflictException;
import com.unicheck.usuario.domain.Aluno;
import com.unicheck.usuario.repository.AlunoRepository;
import com.unicheck.usuario.service.IdentidadeService;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

/**
 * @apiNote Serviço que lida com a lógica de negócio da entidade "Beneficio",
 *          incluindo os favoritos do aluno.
 * @author Mateus Alves Costa
 * @since 18.08.2026
 * @updated 19.09.2026
 */
@Service
@RequiredArgsConstructor
public class BeneficioService {

    private final BeneficioRepository beneficioRepository;
    private final CategoriaRepository categoriaRepository;
    private final BeneficioFavoritoRepository favoritoRepository;
    private final AlunoRepository alunoRepository;
    private final IdentidadeService identidadeService;

    /**
     * @apiNote Busca da listagem, com filtros opcionais de texto, tipo e categoria.
     *          Benefícios de categoria desativada continuam aparecendo: desativar
     *          só impede novos vínculos, não esconde os existentes.
     *
     *          O favorito não sai da entidade; é resolvido aqui numa consulta só
     *          para a página inteira, a partir do aluno autenticado.
     * @param termo       --> Termo de busca livre, ou null/vazio para não filtrar.
     * @param tipo        --> Tipo do benefício, ou null para não filtrar.
     * @param categoriaId --> ID da categoria, ou null para não filtrar.
     * @param ra          --> RA do usuário autenticado; se não for um aluno, nenhum
     *                    benefício vem marcado como favorito.
     * @param pageable    --> Página e tamanho; a ordenação é fixa na consulta.
     * @return --> Retorna uma página com os benefícios que atendem aos filtros.
     * @author Mateus Alves Costa
     * @since 18.08.2026
     * @updated 19.09.2026
     */
    @Transactional(readOnly = true)
    public Page<BeneficioSummaryDTO> buscar(String termo, TipoBeneficio tipo, Long categoriaId, String ra,
            Pageable pageable) {
        String padrao = null;
        if(termo != null && !termo.isBlank()) padrao = "%" + termo.trim().toLowerCase() + "%";

        String tipoBuscado = null;
        if(tipo != null) tipoBuscado = tipo.name();

        Page<Beneficio> pagina = beneficioRepository.buscar(padrao, tipoBuscado, categoriaId, pageable);
        Set<Long> favoritos = favoritosDoAluno(ra, pagina.getContent());

        return pagina.map(beneficio -> toDto(beneficio, favoritos.contains(beneficio.getId())));
    }

    /**
     * @apiNote Busca um benefício pelo seu ID, já com categorias, palavras-chave e
     *          passos, que é o conteúdo do card expandido.
     * @param id --> ID do benefício a ser buscado.
     * @return --> Retorna os dados completos do benefício encontrado.
     * @throws EntityNotFoundException --> Lançada caso o benefício com o ID
     *                                   fornecido não seja encontrado.
     * @author Mateus Alves Costa
     * @since 18.08.2026
     */
    @Transactional(readOnly = true)
    public BeneficioResponseDTO findById(Long id) {
        Beneficio beneficio = beneficioRepository.findWithDetalhesById(id)
                .orElseThrow(() -> new EntityNotFoundException("Benefício não encontrado com id: " + id));
        return toDto(beneficio);
    }

    /**
     * @apiNote Cria um novo benefício.
     * @param dto --> Dados para criação do benefício.
     * @return --> Retorna os dados do benefício criado.
     * @throws ResourceConflictException --> Lançada caso o nome já esteja
     *                                   cadastrado, ou caso alguma das categorias
     *                                   informadas esteja desativada.
     * @throws EntityNotFoundException --> Lançada caso algum dos IDs de categoria
     *                                   informados não exista.
     * @author Mateus Alves Costa
     * @since 18.08.2026
     * @updated 19.09.2026
     */
    @Transactional
    public BeneficioResponseDTO create(BeneficioRequestDTO dto) {
        if(beneficioRepository.existsByNomeIgnoreCase(dto.nome())) {
            throw new ResourceConflictException("Já existe um benefício com o nome '" + dto.nome().trim() + "'.");
        }

        Beneficio beneficio = new Beneficio();
        beneficio.setNome(dto.nome().trim());
        beneficio.setChamada(dto.chamada().trim());
        beneficio.setDescricao(dto.descricao().trim());
        beneficio.setUrl(dto.url().trim());
        beneficio.setTipo(dto.tipo());
        beneficio.setPublicoElegivel(dto.publicoElegivel().trim());
        if(dto.iconeUrl() != null && !dto.iconeUrl().isBlank()) beneficio.setIconeUrl(dto.iconeUrl().trim());
        if(dto.observacaoElegibilidade() != null && !dto.observacaoElegibilidade().isBlank()) beneficio.setObservacaoElegibilidade(dto.observacaoElegibilidade().trim());
        if(dto.avisoImportante() != null && !dto.avisoImportante().isBlank()) beneficio.setAvisoImportante(dto.avisoImportante().trim());
        if(dto.disponibilidade() != null && !dto.disponibilidade().isBlank()) beneficio.setDisponibilidade(dto.disponibilidade().trim());

        substituirCategorias(beneficio, dto.categoriaIds());
        substituirPalavrasChave(beneficio, dto.palavrasChave());
        substituirPassos(beneficio, dto.passos());

        return toDto(beneficioRepository.save(beneficio));
    }

    /**
     * @apiNote Atualiza um benefício existente. Campo ausente significa "não mexe".
     *
     *          Uma categoria desativada que o benefício já tinha pode permanecer; o
     *          que a edição não pode é acrescentar uma categoria desativada nova.
     * @param id  --> ID do benefício a ser atualizado.
     * @param dto --> Dados para atualização do benefício.
     * @return --> Retorna os dados do benefício atualizado.
     * @throws EntityNotFoundException --> Lançada caso o benefício não exista, ou
     *                                   caso algum dos IDs de categoria informados
     *                                   não exista.
     * @throws ResourceConflictException --> Lançada caso o nome já esteja cadastrado
     *                                   em outro benefício, ou caso a edição tente
     *                                   acrescentar categoria desativada.
     * @author Mateus Alves Costa
     * @since 18.08.2026
     * @updated 19.09.2026
     */
    @Transactional
    public BeneficioResponseDTO update(Long id, BeneficioRequestDTO dto) {
        Beneficio beneficio = beneficioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Benefício não encontrado com id: " + id));

        if(dto.nome() != null && !dto.nome().isBlank()
                && beneficioRepository.existsByNomeIgnoreCaseAndIdNot(dto.nome().trim(), id)) {
            throw new ResourceConflictException("Já existe um benefício com o nome '" + dto.nome().trim() + "'.");
        }

        if(dto.nome() != null && !dto.nome().isBlank()) beneficio.setNome(dto.nome().trim());
        if(dto.chamada() != null && !dto.chamada().isBlank()) beneficio.setChamada(dto.chamada().trim());
        if(dto.descricao() != null && !dto.descricao().isBlank()) beneficio.setDescricao(dto.descricao().trim());
        if(dto.url() != null && !dto.url().isBlank()) beneficio.setUrl(dto.url().trim());
        if(dto.publicoElegivel() != null && !dto.publicoElegivel().isBlank()) beneficio.setPublicoElegivel(dto.publicoElegivel().trim());
        if(dto.tipo() != null) beneficio.setTipo(dto.tipo());
        if(dto.iconeUrl() != null) beneficio.setIconeUrl(dto.iconeUrl().isBlank() ? null : dto.iconeUrl().trim());
        if(dto.observacaoElegibilidade() != null) beneficio.setObservacaoElegibilidade(dto.observacaoElegibilidade().isBlank() ? null : dto.observacaoElegibilidade().trim());
        if(dto.avisoImportante() != null) beneficio.setAvisoImportante(dto.avisoImportante().isBlank() ? null : dto.avisoImportante().trim());
        if(dto.disponibilidade() != null) beneficio.setDisponibilidade(dto.disponibilidade().isBlank() ? null : dto.disponibilidade().trim());

        substituirCategorias(beneficio, dto.categoriaIds());
        substituirPalavrasChave(beneficio, dto.palavrasChave());
        substituirPassos(beneficio, dto.passos());

        return toDto(beneficioRepository.save(beneficio));
    }

    /**
     * @apiNote Remove um benefício em definitivo, junto com suas palavras-chave,
     *          passos e marcações de favorito.
     * @param id --> ID do benefício a ser removido.
     * @throws EntityNotFoundException --> Lançada caso o benefício com o ID
     *                                   fornecido não seja encontrado.
     * @author Mateus Alves Costa
     * @since 18.08.2026
     */
    @Transactional
    public void delete(Long id) {
        if(!beneficioRepository.existsById(id)) {
            throw new EntityNotFoundException("Benefício não encontrado com id: " + id);
        }
        beneficioRepository.deleteById(id);
    }

    /**
     * @apiNote Marca um benefício como favorito do aluno. É idempotente: favoritar
     *          o que já está favoritado não altera nada e não é erro.
     * @param ra          --> RA do aluno autenticado.
     * @param beneficioId --> ID do benefício a ser favoritado.
     * @throws EntityNotFoundException --> Lançada caso o aluno ou o benefício não
     *                                   sejam encontrados.
     * @author Mateus Alves Costa
     * @since 18.08.2026
     */
    @Transactional
    public void favoritar(String ra, Long beneficioId) {
        Aluno aluno = identidadeService.alunoPorRa(ra);
        Beneficio beneficio = beneficioRepository.findById(beneficioId)
                .orElseThrow(() -> new EntityNotFoundException("Benefício não encontrado com id: " + beneficioId));

        if(favoritoRepository.contarFavorito(aluno.getId(), beneficioId) > 0) {
            return;
        }

        favoritoRepository.save(new BeneficioFavorito(aluno, beneficio));
    }

    /**
     * @apiNote Remove a marcação de favorito do aluno.
     * @param ra          --> RA do aluno autenticado.
     * @param beneficioId --> ID do benefício a ser desfavoritado.
     * @throws EntityNotFoundException --> Lançada caso o aluno não seja
     *                                   encontrado, ou caso o benefício não esteja
     *                                   entre os favoritos dele.
     * @author Mateus Alves Costa
     * @since 18.08.2026
     */
    @Transactional
    public void desfavoritar(String ra, Long beneficioId) {
        Aluno aluno = identidadeService.alunoPorRa(ra);

        if(favoritoRepository.deleteByAlunoAndBeneficio(aluno.getId(), beneficioId) == 0) {
            throw new EntityNotFoundException("O benefício de id " + beneficioId + " não está nos favoritos deste aluno.");
        }
    }

    /**
     * @apiNote Lista os benefícios favoritados pelo aluno. Todos vêm com favorito
     *          igual a true, já que a própria lista é o recorte dos favoritos.
     * @param ra       --> RA do aluno autenticado.
     * @param pageable --> Página e tamanho; a ordenação é fixa na consulta.
     * @return --> Retorna uma página com os benefícios favoritados.
     * @throws EntityNotFoundException --> Lançada caso o aluno não seja
     *                                   encontrado.
     * @author Mateus Alves Costa
     * @since 18.08.2026
     */
    @Transactional(readOnly = true)
    public Page<BeneficioSummaryDTO> listarFavoritos(String ra, Pageable pageable) {
        Aluno aluno = identidadeService.alunoPorRa(ra);

        return favoritoRepository.findBeneficiosPorAluno(aluno.getId(), pageable)
                .map(beneficio -> toDto(beneficio, true));
    }

    /**
     * @apiNote Descobre, entre os beneficios de uma pagina, quais o aluno favoritou.
     *
     *          Uma consulta so para a pagina inteira. Quem nao tem perfil de aluno, como o
     *          ADMIN, nao tem favoritos, e pagina vazia nem chega a consultar: uma clausula
     *          IN com lista vazia e SQL invalido em varios bancos.
     * @param ra         --> RA do usuario autenticado.
     * @param beneficios --> Beneficios da pagina corrente.
     * @return --> Retorna os IDs favoritados, ou vazio quando nao ha o que consultar.
     * @author Mateus Alves Costa
     * @since 20.09.2026
     */
    private Set<Long> favoritosDoAluno(String ra, List<Beneficio> beneficios) {
        Long alunoId = alunoRepository.findByUsuario_Ra(ra).map(Aluno::getId).orElse(null);
        if(alunoId == null || beneficios.isEmpty()) return Set.of();

        return favoritoRepository.findBeneficioIdsFavoritados(alunoId,
                beneficios.stream().map(Beneficio::getId).toList());
    }

    /**
     * @apiNote Substitui as categorias do beneficio pelas dos ids informados.
     *
     *          Uma categoria desativada que o beneficio ja tinha pode permanecer, porque
     *          desativar nao desfaz vinculo existente. O que nao se pode e acrescentar uma
     *          desativada nova, por isso o conjunto atual e lido antes da troca.
     * @param beneficio --> Beneficio a ter as categorias substituidas.
     * @param ids       --> IDs das categorias, ou null para nao mexer no vinculo.
     * @throws EntityNotFoundException   --> Lancada caso algum id nao exista.
     * @throws ResourceConflictException --> Lancada caso a troca acrescente categoria desativada.
     * @author Mateus Alves Costa
     * @since 20.09.2026
     */
    private void substituirCategorias(Beneficio beneficio, Set<Long> ids) {
        if(ids == null) return;

        Set<Long> jaVinculadas = beneficio.getCategorias().stream().map(Categoria::getId)
                .collect(Collectors.toSet());
        List<Categoria> categorias = categoriaRepository.findAllById(ids);

        if(categorias.size() != ids.size()) {
            Set<Long> encontradas = categorias.stream().map(Categoria::getId).collect(Collectors.toSet());
            throw new EntityNotFoundException("Categoria não encontrada com id: " + ids.stream()
                    .filter(id -> !encontradas.contains(id)).map(String::valueOf).collect(Collectors.joining(", ")));
        }

        String desativadas = categorias.stream()
                .filter(categoria -> !categoria.isAtivo() && !jaVinculadas.contains(categoria.getId()))
                .map(Categoria::getNome).sorted().collect(Collectors.joining(", "));
        if(!desativadas.isEmpty()) {
            throw new ResourceConflictException("Não é possível vincular categoria desativada: " + desativadas + ".");
        }

        beneficio.getCategorias().clear();
        beneficio.getCategorias().addAll(categorias);
    }

    /**
     * @apiNote Substitui as palavras-chave do beneficio, descartando as vazias.
     * @param beneficio --> Beneficio a ter as palavras-chave substituidas.
     * @param palavras  --> Palavras recebidas, ou null para nao mexer.
     * @author Mateus Alves Costa
     * @since 20.09.2026
     */
    private void substituirPalavrasChave(Beneficio beneficio, Set<String> palavras) {
        if(palavras == null) return;

        beneficio.getPalavrasChave().clear();
        palavras.stream().filter(palavra -> palavra != null && !palavra.isBlank()).map(String::trim)
                .forEach(beneficio.getPalavrasChave()::add);
    }

    /**
     * @apiNote Substitui os passos do beneficio, ja na ordem em que a tela os numera.
     * @param beneficio --> Beneficio a ter os passos substituidos.
     * @param passos    --> Passos recebidos, ou null para nao mexer.
     * @author Mateus Alves Costa
     * @since 20.09.2026
     */
    private void substituirPassos(Beneficio beneficio, List<PassoBeneficioDTO> passos) {
        if(passos == null) return;

        beneficio.getPassos().clear();
        passos.stream().filter(Objects::nonNull).sorted(Comparator.comparing(PassoBeneficioDTO::ordem))
                .map(passo -> new PassoBeneficio(passo.ordem(), passo.titulo(), passo.descricao()))
                .forEach(beneficio.getPassos()::add);
    }

    /**
     * @apiNote Converte uma entidade Beneficio no resumo exibido no card da
     *          listagem.
     *
     *          As coleções são copiadas para fora das coleções gerenciadas pelo
     *          Hibernate, de modo que a serialização não dependa mais da sessão.
     *
     *          O favorito não sai da entidade: depende do aluno autenticado, e quem
     *          sabe disso é quem chama.
     * @param beneficio --> Entidade Beneficio a ser convertida; deve vir com as
     *                  coleções acessíveis, ou seja, dentro da transação.
     * @param favorito  --> Indica se o aluno autenticado favoritou este benefício.
     * @return --> Retorna os dados do card do benefício.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    private BeneficioSummaryDTO toDto(Beneficio beneficio, boolean favorito) {
        return new BeneficioSummaryDTO(
                beneficio.getId(),
                beneficio.getIconeUrl(),
                beneficio.getNome(),
                beneficio.getChamada(),
                beneficio.getDescricao(),
                beneficio.getUrl(),
                beneficio.getTipo(),
                beneficio.getPublicoElegivel(),
                beneficio.getPalavrasChave() == null ? Set.of() : Set.copyOf(beneficio.getPalavrasChave()),
                beneficio.getCategorias() == null ? Set.of()
                        : beneficio.getCategorias().stream()
                                .map(categoria -> new CategoriaResponseDTO(categoria.getId(), categoria.getNome(),
                                        categoria.isAtivo()))
                                .collect(Collectors.toUnmodifiableSet()),
                favorito);
    }

    /**
     * @apiNote Converte uma entidade Beneficio no conteúdo do card expandido.
     *
     *          Os passos mantêm a ordem em que a entidade os carregou (OrderBy),
     *          porque é a ordem em que a tela numera "Como conseguir".
     * @param beneficio --> Entidade Beneficio a ser convertida; deve vir com as
     *                  coleções acessíveis, ou seja, dentro da transação.
     * @return --> Retorna os dados completos do benefício.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    private BeneficioResponseDTO toDto(Beneficio beneficio) {
        return new BeneficioResponseDTO(
                beneficio.getId(),
                beneficio.getIconeUrl(),
                beneficio.getNome(),
                beneficio.getChamada(),
                beneficio.getDescricao(),
                beneficio.getUrl(),
                beneficio.getTipo(),
                beneficio.getPublicoElegivel(),
                beneficio.getObservacaoElegibilidade(),
                beneficio.getAvisoImportante(),
                beneficio.getDisponibilidade(),
                beneficio.getPalavrasChave() == null ? Set.of() : Set.copyOf(beneficio.getPalavrasChave()),
                beneficio.getPassos() == null ? List.of()
                        : beneficio.getPassos().stream()
                                .map(passo -> new PassoBeneficioDTO(passo.getOrdem(), passo.getTitulo(),
                                        passo.getDescricao()))
                                .toList(),
                beneficio.getCategorias() == null ? Set.of()
                        : beneficio.getCategorias().stream()
                                .map(categoria -> new CategoriaResponseDTO(categoria.getId(), categoria.getNome(),
                                        categoria.isAtivo()))
                                .collect(Collectors.toUnmodifiableSet()));
    }
}
