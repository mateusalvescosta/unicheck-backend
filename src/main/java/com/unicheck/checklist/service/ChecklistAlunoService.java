package com.unicheck.checklist.service;

import com.unicheck.checklist.domain.*;
import com.unicheck.checklist.dto.*;
import com.unicheck.checklist.enumeration.*;
import com.unicheck.checklist.repository.*;
import com.unicheck.exception.*;
import com.unicheck.gameficacao.service.GameficacaoService;
import com.unicheck.usuario.domain.Aluno;
import com.unicheck.usuario.service.IdentidadeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Implementa a jornada individual do aluno. A identidade sempre vem do subject do JWT,
 * e a conclusão coordena progresso, XP, bônus e horas em uma única transação.
 */
@Service @RequiredArgsConstructor
public class ChecklistAlunoService {
    private final ChecklistRepository checklistRepository;
    private final ItemChecklistRepository itemRepository;
    private final ProgressoItemRepository progressoRepository;
    private final ChecklistConcluidoRepository concluidoRepository;
    private final ChecklistPrerequisitoRepository prerequisitoRepository;
    private final IdentidadeService identidadeService;
    private final GameficacaoService gameficacaoService;

    @Transactional(readOnly = true)
    public List<ChecklistAlunoResponseDTO> listar(String ra) {
        Aluno aluno = identidadeService.alunoPorRa(ra);
        List<Checklist> publicados = checklistRepository.findAllByStatusOrderByOrdem(StatusChecklist.PUBLICADO);
        List<Checklist> arquivadosIniciados = checklistRepository.findAll().stream()
                .filter(c -> c.getStatus() == StatusChecklist.ARQUIVADO)
                .filter(c -> progressoRepository.existsByIdAlunoIdAndIdItemChecklistId(aluno.getId(), c.getId())
                        || concluidoRepository.existsByIdAlunoIdAndIdChecklistId(aluno.getId(), c.getId())).toList();
        List<Checklist> visiveis = new ArrayList<>(publicados); visiveis.addAll(arquivadosIniciados);
        visiveis.sort(Comparator.comparing(Checklist::getOrdem));
        return visiveis.stream().map(c -> resposta(c, aluno, false)).toList();
    }

    @Transactional(readOnly = true)
    public ChecklistAlunoResponseDTO buscar(Long id, String ra) {
        Aluno aluno = identidadeService.alunoPorRa(ra);
        Checklist c = checklistRepository.buscarDetalhado(id).orElseThrow(() -> new ResourceNotFoundException("Checklist", id));
        validarVisibilidade(c, aluno);
        ChecklistAlunoResponseDTO base = resposta(c, aluno, true);
        if (base.estado() == EstadoChecklistAluno.BLOQUEADO) throw new ResourceConflictException(base.motivoBloqueio());
        return base;
    }

    /** Cards dos checklists informados, iguais aos de listar(); usado pelas notificações. Ids inexistentes ficam de fora. */
    @Transactional(readOnly = true)
    public Map<Long, ChecklistAlunoResponseDTO> cardsPorIds(Collection<Long> ids, String ra) {
        Aluno aluno = identidadeService.alunoPorRa(ra);
        return checklistRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Checklist::getId, c -> resposta(c, aluno, false)));
    }

    @Transactional
    public ConclusaoEtapaResponseDTO concluir(Long checklistId, Long itemId, String ra) {
        // Checklist é bloqueado antes do Aluno; operações administrativas usam o mesmo lock e não correm com o primeiro progresso.
        Checklist checklist = checklistRepository.bloquearPorId(checklistId)
                .orElseThrow(() -> new ResourceNotFoundException("Checklist", checklistId));
        Aluno aluno = identidadeService.bloquearAlunoPorRa(ra);
        validarVisibilidade(checklist, aluno);
        validarPrerequisitos(checklist, aluno);
        ItemChecklist item = itemRepository.findById(itemId).orElseThrow(() -> new ResourceNotFoundException("Etapa", itemId));
        if (!item.getChecklist().getId().equals(checklistId)) throw new ResourceConflictException("Etapa nao pertence ao checklist informado");

        Optional<ProgressoItem> existente = progressoRepository.findByIdAlunoIdAndIdItemId(aluno.getId(), itemId);
        if (existente.isPresent() && existente.get().getStatus() == StatusProgressoItem.CONCLUIDO) {
            ProgressoItem p = existente.get();
            ChecklistConcluido conclusao = concluidoRepository.bloquear(aluno.getId(), checklistId).orElse(null);
            return retorno(checklist, item, p, conclusao, aluno, true);
        }

        Set<Long> itensConcluidos = progressoRepository
                .findByIdAlunoIdAndIdItemChecklistId(aluno.getId(), checklistId).stream()
                .filter(p -> p.getStatus() == StatusProgressoItem.CONCLUIDO)
                .map(p -> p.getItem().getId()).collect(Collectors.toSet());
        boolean anteriorPendente = itemRepository.findByChecklistIdOrderByOrdem(checklistId).stream()
                .filter(i -> i.getOrdem() < item.getOrdem())
                .anyMatch(i -> !itensConcluidos.contains(i.getId()));
        if (anteriorPendente) throw new ResourceConflictException("Conclua as etapas anteriores primeiro");

        OffsetDateTime agora = OffsetDateTime.now();
        ProgressoItem progresso = existente.orElseGet(() -> new ProgressoItem(aluno, item, StatusProgressoItem.CONCLUIDO, agora));
        progresso.setStatus(StatusProgressoItem.CONCLUIDO); progresso.setDataConclusao(agora); progresso.setXpCreditado(item.getXp());
        progressoRepository.save(progresso);
        gameficacaoService.concederXp(aluno, item.getXp(), agora);

        List<ItemChecklist> itens = itemRepository.findByChecklistIdOrderByOrdem(checklistId);
        boolean ultima = item.getOrdem().equals(itens.getLast().getOrdem());
        ChecklistConcluido conclusao = null;
        if (ultima) {
            // O lock do Aluno e a PK composta tornam etapa, bônus e horas uma única transação idempotente.
            conclusao = concluidoRepository.bloquear(aluno.getId(), checklistId).orElse(null);
            if (conclusao == null) {
                conclusao = new ChecklistConcluido(aluno, checklist, checklist.getHorasComplementares(), agora);
                conclusao.setXpCreditado(checklist.getXp());
                conclusao.setStatusLancamentoHoras(checklist.getHorasComplementares() > 0
                        ? StatusLancamentoHoras.PENDENTE : StatusLancamentoHoras.NAO_APLICAVEL);
                concluidoRepository.save(conclusao);
                gameficacaoService.concederXp(aluno, checklist.getXp(), agora);
            }
        }
        return retorno(checklist, item, progresso, conclusao, aluno, false);
    }

    private void validarVisibilidade(Checklist c, Aluno aluno) {
        if (c.getStatus() == StatusChecklist.RASCUNHO) throw new ResourceNotFoundException("Checklist nao encontrado");
        if (c.getStatus() == StatusChecklist.ARQUIVADO
                && !progressoRepository.existsByIdAlunoIdAndIdItemChecklistId(aluno.getId(), c.getId())
                && !concluidoRepository.existsByIdAlunoIdAndIdChecklistId(aluno.getId(), c.getId()))
            throw new ResourceConflictException("Checklist arquivado nao pode ser iniciado");
    }

    private void validarPrerequisitos(Checklist c, Aluno aluno) {
        List<String> pendentes = prerequisitoRepository.findByIdChecklistId(c.getId()).stream()
                .map(ChecklistPrerequisito::getChecklistRequisito)
                .filter(r -> !concluidoRepository.existsByIdAlunoIdAndIdChecklistId(aluno.getId(), r.getId()))
                .map(Checklist::getNome).toList();
        if (!pendentes.isEmpty()) throw new ResourceConflictException("Prerequisitos pendentes: " + String.join(", ", pendentes));
    }

    private ChecklistAlunoResponseDTO resposta(Checklist c, Aluno aluno, boolean detalhar) {
        List<ItemChecklist> itens = itemRepository.findByChecklistIdOrderByOrdem(c.getId());
        Map<Long, ProgressoItem> progressos = progressoRepository.findByIdAlunoIdAndIdItemChecklistId(aluno.getId(), c.getId()).stream()
                .collect(Collectors.toMap(p -> p.getItem().getId(), Function.identity()));
        boolean concluido = concluidoRepository.existsByIdAlunoIdAndIdChecklistId(aluno.getId(), c.getId());
        List<ChecklistPrerequisito> requisitos = prerequisitoRepository.findByIdChecklistId(c.getId());
        List<PrerequisitoResponseDTO> requisitoDtos = requisitos.stream().map(p -> {
            Checklist r = p.getChecklistRequisito();
            return new PrerequisitoResponseDTO(r.getId(), r.getNome(), concluidoRepository.existsByIdAlunoIdAndIdChecklistId(aluno.getId(), r.getId()));
        }).toList();
        List<String> pendentes = requisitoDtos.stream().filter(p -> !p.concluido()).map(PrerequisitoResponseDTO::nome).toList();
        EstadoChecklistAluno estado = concluido ? EstadoChecklistAluno.CONCLUIDO
                : !pendentes.isEmpty() ? EstadoChecklistAluno.BLOQUEADO
                : progressos.isEmpty() ? EstadoChecklistAluno.NAO_INICIADO : EstadoChecklistAluno.EM_ANDAMENTO;
        String motivo = pendentes.isEmpty() ? null : "Conclua antes: " + String.join(", ", pendentes);
        ItemChecklist proxima = estado == EstadoChecklistAluno.BLOQUEADO || concluido ? null : itens.stream()
                .filter(i -> !progressos.containsKey(i.getId())).findFirst().orElse(null);
        List<ItemChecklistResponseDTO> etapas = !detalhar || estado == EstadoChecklistAluno.BLOQUEADO ? List.of() : itens.stream().map(i -> {
            ProgressoItem p = progressos.get(i.getId());
            return new ItemChecklistResponseDTO(i.getId(), i.getTitulo(), i.getDescricao(), i.getConteudo(), i.getOrdem(), i.getXp(),
                    p == null ? StatusProgressoItem.PENDENTE : p.getStatus(), p == null ? null : p.getDataConclusao(),
                    p == null ? null : p.getXpCreditado(), proxima != null && proxima.getId().equals(i.getId()));
        }).toList();
        int xpEtapas = itens.stream().mapToInt(ItemChecklist::getXp).sum(); long feitas = progressos.values().stream().filter(p -> p.getStatus() == StatusProgressoItem.CONCLUIDO).count();
        int percentual = itens.isEmpty() ? 0 : (int) (feitas * 100 / itens.size());
        return new ChecklistAlunoResponseDTO(c.getId(), c.getNome(), c.getDescricao(), c.getOrdem(), c.getStatus(), estado, motivo,
                itens.size(), feitas, percentual, xpEtapas, c.getXp(), xpEtapas + c.getXp(), c.getHorasComplementares(),
                proxima == null ? null : proxima.getId(), requisitoDtos, etapas);
    }

    private ConclusaoEtapaResponseDTO retorno(Checklist c, ItemChecklist i, ProgressoItem p, ChecklistConcluido cc, Aluno a, boolean repetida) {
        return new ConclusaoEtapaResponseDTO(c.getId(), i.getId(), p.getDataConclusao(), p.getXpCreditado(), cc != null,
                cc == null ? 0 : cc.getXpCreditado(), cc == null ? 0 : cc.getHorasCreditadas(), a.getXpTotal(), repetida);
    }
}
