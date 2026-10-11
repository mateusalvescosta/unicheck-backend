package com.unicheck.manual.service;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.unicheck.exception.ResourceConflictException;
import com.unicheck.manual.domain.AtalhoManual;
import com.unicheck.manual.domain.LocalAtendimento;
import com.unicheck.manual.domain.Manual;
import com.unicheck.manual.domain.PassoManual;
import com.unicheck.manual.dto.AtalhoManualDTO;
import com.unicheck.manual.dto.CategoriaManualDTO;
import com.unicheck.manual.dto.LocalAtendimentoDTO;
import com.unicheck.manual.dto.ManualReferenciaDTO;
import com.unicheck.manual.dto.ManualRequestDTO;
import com.unicheck.manual.dto.ManualResponseDTO;
import com.unicheck.manual.dto.ManualSummaryDTO;
import com.unicheck.manual.dto.PassoManualDTO;
import com.unicheck.manual.enumeration.CategoriaManual;
import com.unicheck.manual.repository.AtalhoManualRepository;
import com.unicheck.manual.repository.ManualRepository;
import com.unicheck.notificacao.enumeration.TipoNotificacao;
import com.unicheck.notificacao.service.NotificacaoService;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

/**
 * @apiNote Serviço que lida com a lógica de negócio do Manual do Aluno: páginas
 *          de manual, busca, categorias e "Atalhos do dia a dia".
 * @author Mateus Alves Costa
 * @since 27.09.2026
 */
@Service
@RequiredArgsConstructor
public class ManualService {

    private final ManualRepository manualRepository;
    private final AtalhoManualRepository atalhoRepository;
    private final NotificacaoService notificacaoService;

    /**
     * @apiNote Busca de "Qual é a sua dúvida?" e listagem por assunto. Sem termo e
     *          sem categoria, devolve todos os manuais na ordem da sequência.
     * @param termo     --> Termo de busca livre, ou null/vazio para não filtrar.
     * @param categoria --> Categoria, ou null para não filtrar.
     * @param pageable  --> Página e tamanho; a ordenação é fixa na consulta.
     * @return --> Retorna uma página com os manuais encontrados.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Transactional(readOnly = true)
    public Page<ManualSummaryDTO> buscar(String termo, CategoriaManual categoria, Pageable pageable) {
        String padrao = null;
        if(termo != null && !termo.isBlank()) padrao = "%" + termo.trim().toLowerCase() + "%";

        String categoriaBuscada = null;
        if(categoria != null) categoriaBuscada = categoria.name();

        return manualRepository.buscar(padrao, categoriaBuscada, pageable).map(this::toSummary);
    }

    /**
     * @apiNote Lista as categorias de "Encontre por assunto" na ordem da tela, com
     *          a quantidade de manuais de cada uma. Categoria sem manual vem com
     *          zero, para que a tela decida se a exibe.
     *
     *          É uma contagem por categoria; como são poucas e a coluna tem
     *          índice, isso sai mais simples que agrupar numa consulta só.
     * @return --> Retorna todas as categorias.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Transactional(readOnly = true)
    public List<CategoriaManualDTO> listarCategorias() {
        return Arrays.stream(CategoriaManual.values())
                .map(categoria -> new CategoriaManualDTO(categoria, categoria.getNome(), categoria.getNomeCurto(),
                        categoria.getDescricao(), manualRepository.countByCategoria(categoria)))
                .toList();
    }

    /**
     * @apiNote Busca a página completa de um manual, já com "Conteúdo anterior" e
     *          "Próximo conteúdo" resolvidos pela ordem.
     * @param id --> ID do manual a ser buscado.
     * @return --> Retorna os dados completos do manual.
     * @throws EntityNotFoundException --> Lançada caso o manual não exista.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Transactional(readOnly = true)
    public ManualResponseDTO findById(Long id) {
        return toDto(buscarManual(id));
    }

    /**
     * @apiNote Cria um novo manual. Sem ordem, ele entra no fim da sequência; com
     *          ordem, os manuais a partir dela descem uma posição.
     * @param dto --> Dados para criação do manual.
     * @return --> Retorna os dados do manual criado.
     * @throws ResourceConflictException --> Lançada caso o título já esteja em uso.
     * @throws EntityNotFoundException   --> Lançada caso algum manual referenciado
     *                                   não exista.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Transactional
    public ManualResponseDTO create(ManualRequestDTO dto) {
        if(manualRepository.existsByTituloIgnoreCase(dto.titulo().trim())) {
            throw new ResourceConflictException("Já existe um manual com o título '" + dto.titulo().trim() + "'.");
        }

        Manual manual = new Manual();
        aplicar(manual, dto);

        int ultima = manualRepository.maiorOrdem();
        int ordem = ultima + 1;
        if(dto.ordem() != null && dto.ordem() <= ultima) {
            ordem = dto.ordem();
            manualRepository.deslocarOrdem(ordem, ultima, 1);
        }
        manual.setOrdem(ordem);

        Manual salvo = manualRepository.save(manual);
        notificacaoService.notificarAlunos(TipoNotificacao.MANUAL_NOVO, salvo.getId(), "Novo manual: " + salvo.getTitulo());
        return toDto(salvo);
    }

    /**
     * @apiNote Atualiza um manual por completo: campo opcional ausente é removido.
     *          Com ordem diferente da atual, o manual muda de posição e os que
     *          estão entre a posição antiga e a nova se deslocam uma casa.
     * @param id  --> ID do manual a ser atualizado.
     * @param dto --> Dados completos do manual.
     * @return --> Retorna os dados do manual atualizado.
     * @throws EntityNotFoundException   --> Lançada caso o manual, ou algum manual
     *                                   referenciado, não exista.
     * @throws ResourceConflictException --> Lançada caso o título já esteja em uso
     *                                   por outro manual, ou caso o manual aponte
     *                                   para si mesmo.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Transactional
    public ManualResponseDTO update(Long id, ManualRequestDTO dto) {
        Manual manual = buscarManual(id);

        if(manualRepository.existsByTituloIgnoreCaseAndIdNot(dto.titulo().trim(), id)) {
            throw new ResourceConflictException("Já existe um manual com o título '" + dto.titulo().trim() + "'.");
        }
        if(id.equals(dto.proximoPassoManualId()) || (dto.relacionadoIds() != null && dto.relacionadoIds().contains(id))) {
            throw new ResourceConflictException("Um manual não pode indicar a si mesmo.");
        }

        aplicar(manual, dto);
        if(dto.ordem() != null) mover(manual, dto.ordem());

        return toDto(manualRepository.save(manual));
    }

    /**
     * @apiNote Remove um manual em definitivo e fecha o buraco que ele deixa na
     *          sequência. Ele sai também das listas de relacionados e dos atalhos;
     *          quem o indicava em "Seu próximo passo" fica sem indicação.
     * @param id --> ID do manual a ser removido.
     * @throws EntityNotFoundException --> Lançada caso o manual não exista.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Transactional
    public void delete(Long id) {
        Manual manual = buscarManual(id);
        int ordem = manual.getOrdem();

        for(Manual outro : manualRepository.findQueRelacionam(id)) {
            outro.getRelacionados().remove(manual);
        }
        for(Manual outro : manualRepository.findByProximoPassoManualId(id)) {
            outro.setProximoPassoManual(null);
        }

        notificacaoService.removerDoObjeto(id, TipoNotificacao.MANUAL_NOVO);
        manualRepository.delete(manual);
        manualRepository.deslocarOrdem(ordem + 1, Integer.MAX_VALUE, -1);
    }

    /**
     * @apiNote Lista "Atalhos do dia a dia" na ordem da grade.
     * @return --> Retorna os atalhos configurados.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Transactional(readOnly = true)
    public List<AtalhoManualDTO> listarAtalhos() {
        return atalhoRepository.findAllByOrderByOrdemAsc().stream()
                .map(atalho -> new AtalhoManualDTO(atalho.getManual().getId(), atalho.getNome()))
                .toList();
    }

    /**
     * @apiNote Troca a lista de "Atalhos do dia a dia" pela recebida: apaga os
     *          atalhos atuais e grava os novos, numerando pela posição na lista.
     *          Se algo falhar no meio, a transação desfaz tudo, inclusive a
     *          remoção.
     * @param atalhos --> Nova lista de atalhos; vazia remove todos.
     * @return --> Retorna os atalhos gravados.
     * @throws EntityNotFoundException   --> Lançada caso algum manual não exista.
     * @throws ResourceConflictException --> Lançada caso o mesmo manual apareça
     *                                   em mais de um atalho.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Transactional
    public List<AtalhoManualDTO> substituirAtalhos(List<AtalhoManualDTO> atalhos) {
        if(atalhos.stream().map(AtalhoManualDTO::manualId).distinct().count() != atalhos.size()) {
            throw new ResourceConflictException("Um mesmo manual não pode aparecer em mais de um atalho.");
        }

        atalhoRepository.deleteAllInBatch();

        int ordem = 1;
        for(AtalhoManualDTO atalho : atalhos) {
            Manual manual = buscarManual(atalho.manualId());
            atalhoRepository.save(new AtalhoManual(null, manual, atalho.nome().trim(), ordem++));
        }

        return listarAtalhos();
    }

    /**
     * @apiNote Copia para a entidade todos os campos do DTO, exceto a ordem, que
     *          depende de deslocar os demais manuais e é tratada por quem chama.
     * @param manual --> Manual a ser preenchido.
     * @param dto    --> Dados recebidos.
     * @throws EntityNotFoundException --> Lançada caso algum manual referenciado
     *                                 não exista.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    private void aplicar(Manual manual, ManualRequestDTO dto) {
        manual.setCategoria(dto.categoria());
        manual.setTempoLeitura(dto.tempoLeitura());
        manual.setTitulo(dto.titulo().trim());
        manual.setResumo(dto.resumo().trim());
        manual.setLinkTexto(textoOuNull(dto.linkTexto()));
        manual.setLinkUrl(textoOuNull(dto.linkUrl()));
        manual.setQuandoSeAplica(textoOuNull(dto.quandoSeAplica()));
        manual.setOQueSaber(dto.oQueSaber().trim());
        manual.setResultadoEsperado(textoOuNull(dto.resultadoEsperado()));
        manual.setProximoPassoDescricao(textoOuNull(dto.proximoPassoDescricao()));
        manual.setAvisoAtualizacao(textoOuNull(dto.avisoAtualizacao()));

        substituir(manual.getAtencao(), dto.atencao());
        substituir(manual.getAntesDeComecar(), dto.antesDeComecar());
        substituir(manual.getPrazoERegras(), dto.prazoERegras());
        substituir(manual.getDetalhes(), dto.detalhes());
        substituir(manual.getEntendaMelhor(), dto.entendaMelhor());

        manual.getPassos().clear();
        if(dto.passos() != null) {
            if(dto.passos().stream().map(PassoManualDTO::ordem).distinct().count() != dto.passos().size()) {
                throw new ResourceConflictException("Dois passos de \"Como fazer\" não podem ter a mesma ordem.");
            }
            dto.passos().stream().sorted(Comparator.comparing(PassoManualDTO::ordem))
                    .map(passo -> new PassoManual(passo.ordem(), passo.descricao().trim()))
                    .forEach(manual.getPassos()::add);
        }

        manual.getOndeResolver().clear();
        if(dto.ondeResolver() != null) {
            dto.ondeResolver().stream()
                    .map(local -> new LocalAtendimento(local.nome().trim(), textoOuNull(local.url())))
                    .forEach(manual.getOndeResolver()::add);
        }

        manual.setProximoPassoManual(null);
        if(dto.proximoPassoManualId() != null) manual.setProximoPassoManual(buscarManual(dto.proximoPassoManualId()));

        List<Long> relacionadoIds = List.of();
        if(dto.relacionadoIds() != null) relacionadoIds = dto.relacionadoIds();
        if(new HashSet<>(relacionadoIds).size() != relacionadoIds.size()) {
            throw new ResourceConflictException("Um mesmo manual não pode ser relacionado duas vezes.");
        }
        Map<Long, Manual> relacionados = buscarManuais(relacionadoIds);
        manual.getRelacionados().clear();
        relacionadoIds.forEach(relacionadoId -> manual.getRelacionados().add(relacionados.get(relacionadoId)));
    }

    /**
     * @apiNote Leva o manual para a nova posição, deslocando uma casa os que
     *          ficam entre a posição antiga e a nova. Ordem além do fim da
     *          sequência vira a última posição.
     * @param manual --> Manual já persistido a ser movido.
     * @param nova   --> Posição desejada.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    private void mover(Manual manual, int nova) {
        int atual = manual.getOrdem();
        nova = Math.min(nova, manualRepository.maiorOrdem());
        if(nova == atual) return;

        if(nova < atual) manualRepository.deslocarOrdem(nova, atual - 1, 1);
        else manualRepository.deslocarOrdem(atual + 1, nova, -1);
        manual.setOrdem(nova);
    }

    /**
     * @apiNote Substitui uma lista de textos do manual, descartando as pontas em
     *          branco.
     * @param destino --> Lista gerenciada pelo Hibernate.
     * @param origem  --> Textos recebidos, ou null para esvaziar.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    private void substituir(List<String> destino, List<String> origem) {
        destino.clear();
        if(origem != null) origem.stream().map(String::trim).forEach(destino::add);
    }

    private Manual buscarManual(Long id) {
        return manualRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Manual não encontrado com id: " + id));
    }

    /**
     * @apiNote Carrega os manuais dos ids informados numa consulta só.
     * @param ids --> IDs a carregar.
     * @return --> Retorna os manuais indexados pelo id.
     * @throws EntityNotFoundException --> Lançada listando os ids que não existem.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    private Map<Long, Manual> buscarManuais(List<Long> ids) {
        if(ids.isEmpty()) return Map.of();

        Map<Long, Manual> manuais = manualRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Manual::getId, Function.identity()));
        Set<Long> faltando = new HashSet<>(ids);
        faltando.removeAll(manuais.keySet());
        if(!faltando.isEmpty()) {
            throw new EntityNotFoundException("Manual não encontrado com id: " + faltando.stream().sorted()
                    .map(String::valueOf).collect(Collectors.joining(", ")));
        }
        return manuais;
    }

    private String textoOuNull(String valor) {
        if(valor == null || valor.isBlank()) return null;
        return valor.trim();
    }

    /** Cards dos manuais informados, no formato da busca; usado pelas notificações. Ids inexistentes ficam de fora. */
    @Transactional(readOnly = true)
    public Map<Long, ManualSummaryDTO> resumosPorIds(Collection<Long> ids) {
        return manualRepository.findAllById(ids).stream().collect(Collectors.toMap(Manual::getId, this::toSummary));
    }

    private ManualSummaryDTO toSummary(Manual manual) {
        return new ManualSummaryDTO(manual.getId(), manual.getOrdem(), manual.getCategoria(),
                manual.getCategoria().getNome(), manual.getTitulo(), manual.getResumo(), manual.getTempoLeitura());
    }

    private ManualReferenciaDTO toReferencia(Manual manual) {
        if(manual == null) return null;
        return new ManualReferenciaDTO(manual.getId(), manual.getTitulo(), manual.getCategoria(),
                manual.getCategoria().getNome());
    }

    /**
     * @apiNote Converte o manual na página completa. As coleções são copiadas para
     *          fora das coleções gerenciadas, de modo que a serialização não
     *          dependa mais da sessão.
     * @param manual --> Manual a converter; deve vir dentro da transação.
     * @return --> Retorna os dados completos do manual.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    private ManualResponseDTO toDto(Manual manual) {
        return new ManualResponseDTO(
                manual.getId(),
                manual.getOrdem(),
                manual.getCategoria(),
                manual.getCategoria().getNome(),
                manual.getTempoLeitura(),
                manual.getTitulo(),
                manual.getResumo(),
                manual.getLinkTexto(),
                manual.getLinkUrl(),
                manual.getQuandoSeAplica(),
                manual.getOQueSaber(),
                List.copyOf(manual.getAtencao()),
                List.copyOf(manual.getAntesDeComecar()),
                manual.getPassos().stream()
                        .map(passo -> new PassoManualDTO(passo.getOrdem(), passo.getDescricao())).toList(),
                manual.getOndeResolver().stream()
                        .map(local -> new LocalAtendimentoDTO(local.getNome(), local.getUrl())).toList(),
                List.copyOf(manual.getPrazoERegras()),
                manual.getResultadoEsperado(),
                List.copyOf(manual.getDetalhes()),
                List.copyOf(manual.getEntendaMelhor()),
                manual.getProximoPassoDescricao(),
                toReferencia(manual.getProximoPassoManual()),
                manual.getAvisoAtualizacao(),
                manual.getRelacionados().stream().map(this::toReferencia).toList(),
                manualRepository.findFirstByOrdemLessThanOrderByOrdemDesc(manual.getOrdem())
                        .map(this::toReferencia).orElse(null),
                manualRepository.findFirstByOrdemGreaterThanOrderByOrdemAsc(manual.getOrdem())
                        .map(this::toReferencia).orElse(null));
    }
}
