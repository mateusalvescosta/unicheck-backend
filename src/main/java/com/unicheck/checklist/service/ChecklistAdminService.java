package com.unicheck.checklist.service;

import com.unicheck.checklist.domain.*;
import com.unicheck.checklist.dto.*;
import com.unicheck.checklist.enumeration.StatusChecklist;
import com.unicheck.checklist.repository.*;
import com.unicheck.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Mantém o template das fases. Conteúdo editorial pode ser corrigido, enquanto mudanças
 * estruturais são bloqueadas após o primeiro progresso para preservar recompensas históricas.
 */
@Service @RequiredArgsConstructor
public class ChecklistAdminService {
    private final ChecklistRepository checklistRepository;
    private final ItemChecklistRepository itemRepository;
    private final ProgressoItemRepository progressoRepository;
    private final ChecklistConcluidoRepository concluidoRepository;
    private final ChecklistPrerequisitoRepository prerequisitoRepository;

    @Transactional
    public ChecklistAdminResponseDTO criar(ChecklistRequestDTO dto) {
        Checklist c = new Checklist(); aplicar(c, dto); c.setStatus(StatusChecklist.RASCUNHO);
        return resposta(checklistRepository.save(c));
    }

    @Transactional(readOnly = true)
    public List<ChecklistAdminResponseDTO> listar() {
        return checklistRepository.findAll().stream().map(this::resposta).toList();
    }

    @Transactional(readOnly = true)
    public ChecklistAdminResponseDTO buscar(Long id) { return resposta(buscarDetalhado(id)); }

    @Transactional
    public ChecklistAdminResponseDTO atualizar(Long id, ChecklistRequestDTO dto) {
        Checklist c = bloquear(id); exigirEditavel(c);
        boolean estrutural = !Objects.equals(c.getNome(), dto.nome()) || !Objects.equals(c.getOrdem(), dto.ordem()) || !Objects.equals(c.getXp(), dto.xp())
                || !Objects.equals(c.getHorasComplementares(), dto.horasComplementares());
        if (estrutural) exigirSemProgresso(c);
        aplicar(c, dto);
        if (c.getStatus() == StatusChecklist.PUBLICADO) validarPublicacao(c);
        return resposta(c);
    }

    @Transactional
    public ItemChecklistResponseDTO adicionarItem(Long checklistId, ItemChecklistRequestDTO dto) {
        Checklist c = bloquear(checklistId); exigirEditavel(c); exigirSemProgresso(c);
        if (itemRepository.existsByChecklistIdAndOrdem(checklistId, dto.ordem()))
            throw new ResourceConflictException("Ja existe etapa com esta ordem no checklist");
        ItemChecklist item = new ItemChecklist(); item.setChecklist(c); aplicar(item, dto);
        itemRepository.save(item);
        if (c.getStatus() == StatusChecklist.PUBLICADO) validarPublicacao(c);
        return itemResposta(item);
    }

    @Transactional
    public ItemChecklistResponseDTO atualizarItem(Long checklistId, Long itemId, ItemChecklistRequestDTO dto) {
        Checklist c = bloquear(checklistId); exigirEditavel(c);
        ItemChecklist item = itemDoChecklist(c, itemId);
        boolean possuiProgresso = progressoRepository.existsByIdItemChecklistId(checklistId);
        if (possuiProgresso && (!Objects.equals(item.getTitulo(), dto.titulo())
                || !Objects.equals(item.getOrdem(), dto.ordem()) || !Objects.equals(item.getXp(), dto.xp())))
            throw new ResourceConflictException("Apos o primeiro progresso, somente descricao e conteudo podem ser corrigidos");
        if (!Objects.equals(item.getOrdem(), dto.ordem()) && itemRepository.existsByChecklistIdAndOrdem(checklistId, dto.ordem()))
            throw new ResourceConflictException("Ja existe etapa com esta ordem no checklist");
        aplicar(item, dto);
        if (c.getStatus() == StatusChecklist.PUBLICADO) validarPublicacao(c);
        return itemResposta(item);
    }

    @Transactional
    public void removerItem(Long checklistId, Long itemId) {
        Checklist c = bloquear(checklistId); exigirEditavel(c); exigirSemProgresso(c);
        itemRepository.delete(itemDoChecklist(c, itemId));
        itemRepository.flush();
        if (c.getStatus() == StatusChecklist.PUBLICADO) validarPublicacao(c);
    }

    @Transactional
    public ChecklistAdminResponseDTO adicionarPrerequisito(Long id, Long requisitoId) {
        List<Checklist> bloqueados = checklistRepository.bloquearTodos();
        Checklist c = encontrarBloqueado(bloqueados, id); exigirEditavel(c); exigirSemProgresso(c);
        if (id.equals(requisitoId)) throw new ResourceConflictException("Checklist nao pode depender de si mesmo");
        Checklist requisito = encontrarBloqueado(bloqueados, requisitoId);
        if (prerequisitoRepository.existsByIdChecklistIdAndIdChecklistRequisitoId(id, requisitoId)) return resposta(c);
        if (alcanca(requisitoId, id, new HashSet<>())) throw new ResourceConflictException("Prerequisito criaria um ciclo");
        prerequisitoRepository.save(new ChecklistPrerequisito(c, requisito));
        if (c.getStatus() == StatusChecklist.PUBLICADO) validarPublicacao(c);
        return resposta(c);
    }

    @Transactional
    public void removerPrerequisito(Long id, Long requisitoId) {
        Checklist c = encontrarBloqueado(checklistRepository.bloquearTodos(), id); exigirEditavel(c); exigirSemProgresso(c);
        prerequisitoRepository.deleteByIdChecklistIdAndIdChecklistRequisitoId(id, requisitoId);
    }

    @Transactional
    public ChecklistAdminResponseDTO publicar(Long id) {
        Checklist c = encontrarBloqueado(checklistRepository.bloquearTodos(), id);
        if (c.getStatus() == StatusChecklist.ARQUIVADO) throw new ResourceConflictException("Checklist arquivado nao pode ser republicado");
        validarPublicacao(c); c.setStatus(StatusChecklist.PUBLICADO); return resposta(c);
    }

    @Transactional
    public ChecklistAdminResponseDTO arquivar(Long id) {
        Checklist c = encontrarBloqueado(checklistRepository.bloquearTodos(), id);
        boolean bloqueiaTrilha = prerequisitoRepository.findByIdChecklistRequisitoId(id).stream()
                .map(ChecklistPrerequisito::getChecklist).anyMatch(d -> d.getStatus() == StatusChecklist.PUBLICADO);
        if (bloqueiaTrilha) throw new ResourceConflictException("Checklist possui dependentes publicados e nao pode ser arquivado");
        c.setStatus(StatusChecklist.ARQUIVADO); return resposta(c);
    }

    @Transactional
    public void excluir(Long id) {
        Checklist c = bloquear(id);
        if (progressoRepository.existsByIdItemChecklistId(id) || concluidoRepository.existsByIdChecklistId(id))
            throw new ResourceConflictException("Checklist possui historico e nao pode ser excluido");
        if (!prerequisitoRepository.findByIdChecklistRequisitoId(id).isEmpty())
            throw new ResourceConflictException("Checklist e requisito de outra fase e nao pode ser excluido");
        prerequisitoRepository.findByIdChecklistId(id).forEach(prerequisitoRepository::delete);
        checklistRepository.delete(c);
    }

    private void validarPublicacao(Checklist c) {
        List<ItemChecklist> itens = itemRepository.findByChecklistIdOrderByOrdem(c.getId());
        if (itens.isEmpty()) throw new ResourceConflictException("Checklist vazio nao pode ser publicado");
        for (int i = 0; i < itens.size(); i++) if (itens.get(i).getOrdem() != i + 1)
            throw new ResourceConflictException("Etapas devem possuir sequencia continua iniciando em 1");
        for (ChecklistPrerequisito p : prerequisitoRepository.findByIdChecklistId(c.getId()))
            if (p.getChecklistRequisito().getStatus() != StatusChecklist.PUBLICADO)
                throw new ResourceConflictException("Todos os prerequisitos devem estar publicados");
        if (alcanca(c.getId(), c.getId(), new HashSet<>())) throw new ResourceConflictException("Dependencias possuem ciclo");
    }

    private boolean alcanca(Long origem, Long alvo, Set<Long> visitados) {
        if (!visitados.add(origem)) return false;
        for (ChecklistPrerequisito p : prerequisitoRepository.findByIdChecklistId(origem)) {
            Long proximo = p.getChecklistRequisito().getId();
            if (proximo.equals(alvo) || alcanca(proximo, alvo, visitados)) return true;
        }
        return false;
    }

    private void exigirSemProgresso(Checklist c) {
        if (progressoRepository.existsByIdItemChecklistId(c.getId()))
            throw new ResourceConflictException("Estrutura bloqueada apos o primeiro progresso");
    }
    private void exigirEditavel(Checklist c) {
        if (c.getStatus() == StatusChecklist.ARQUIVADO) throw new ResourceConflictException("Checklist arquivado nao pode ser editado");
    }
    private Checklist bloquear(Long id) { return checklistRepository.bloquearPorId(id).orElseThrow(() -> new ResourceNotFoundException("Checklist", id)); }
    private Checklist encontrarBloqueado(List<Checklist> checklists, Long id) {
        return checklists.stream().filter(c -> c.getId().equals(id)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Checklist", id));
    }
    private Checklist buscarDetalhado(Long id) { return checklistRepository.buscarDetalhado(id).orElseThrow(() -> new ResourceNotFoundException("Checklist", id)); }
    private ItemChecklist itemDoChecklist(Checklist c, Long id) { return c.getItens().stream().filter(i -> i.getId().equals(id)).findFirst().orElseThrow(() -> new ResourceNotFoundException("Etapa", id)); }
    private void aplicar(Checklist c, ChecklistRequestDTO d) { c.setNome(d.nome()); c.setDescricao(d.descricao()); c.setOrdem(d.ordem()); c.setXp(d.xp()); c.setHorasComplementares(d.horasComplementares()); }
    private void aplicar(ItemChecklist i, ItemChecklistRequestDTO d) { i.setTitulo(d.titulo()); i.setDescricao(d.descricao()); i.setConteudo(d.conteudo()); i.setOrdem(d.ordem()); i.setXp(d.xp()); }
    private ItemChecklistResponseDTO itemResposta(ItemChecklist i) { return new ItemChecklistResponseDTO(i.getId(), i.getTitulo(), i.getDescricao(), i.getConteudo(), i.getOrdem(), i.getXp(), null, null, null, false); }
    private ChecklistAdminResponseDTO resposta(Checklist c) {
        List<ItemChecklist> itens = itemRepository.findByChecklistIdOrderByOrdem(c.getId());
        int xpEtapas = itens.stream().mapToInt(ItemChecklist::getXp).sum();
        return new ChecklistAdminResponseDTO(c.getId(), c.getNome(), c.getDescricao(), c.getOrdem(), c.getXp(), xpEtapas,
                xpEtapas + c.getXp(), c.getHorasComplementares(), c.getStatus(), progressoRepository.existsByIdItemChecklistId(c.getId()),
                prerequisitoRepository.findByIdChecklistId(c.getId()).stream().map(p -> p.getChecklistRequisito().getId()).toList(),
                itens.stream().map(this::itemResposta).toList());
    }
}
