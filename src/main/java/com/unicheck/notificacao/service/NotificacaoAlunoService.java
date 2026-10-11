package com.unicheck.notificacao.service;

import com.unicheck.beneficio.dto.BeneficioSummaryDTO;
import com.unicheck.beneficio.service.BeneficioService;
import com.unicheck.checklist.dto.ChecklistAlunoResponseDTO;
import com.unicheck.checklist.service.ChecklistAlunoService;
import com.unicheck.exception.ResourceNotFoundException;
import com.unicheck.manual.dto.ManualSummaryDTO;
import com.unicheck.manual.service.ManualService;
import com.unicheck.notificacao.domain.Notificacao;
import com.unicheck.notificacao.dto.NotificacaoResponseDTO;
import com.unicheck.notificacao.dto.PreferenciaNotificacaoDTO;
import com.unicheck.notificacao.dto.TotalNaoLidasResponseDTO;
import com.unicheck.notificacao.enumeration.TipoNotificacao;
import com.unicheck.notificacao.repository.NotificacaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Operações do aluno sobre as próprias notificações. Fica separado do NotificacaoService porque
 * monta objetos com as Services de benefício, manual e checklist, que por sua vez usam o
 * NotificacaoService para notificar; juntos, os dois lados formariam uma dependência circular.
 */
@Service
@RequiredArgsConstructor
public class NotificacaoAlunoService {
    private final NotificacaoRepository notificacaoRepository;
    private final BeneficioService beneficioService;
    private final ManualService manualService;
    private final ChecklistAlunoService checklistAlunoService;

    @Transactional(readOnly = true)
    public Page<NotificacaoResponseDTO> listar(String ra, Boolean lida, Pageable pageable) {
        Page<Notificacao> pagina;
        if (lida == null) {
            pagina = notificacaoRepository.findByUsuarioRaOrderByCriadoEmDescIdDesc(ra, pageable);
        } else {
            pagina = notificacaoRepository.findByUsuarioRaAndLidaOrderByCriadoEmDescIdDesc(ra, lida, pageable);
        }

        // Um lote por tipo, em vez de uma busca por notificação da página.
        Set<Long> beneficioIds = idsDaPagina(pagina, Set.of(TipoNotificacao.BENEFICIO_NOVO));
        Set<Long> manualIds = idsDaPagina(pagina, Set.of(TipoNotificacao.MANUAL_NOVO));
        Set<Long> checklistIds = idsDaPagina(pagina, Set.of(TipoNotificacao.CHECKLIST_NOVO, TipoNotificacao.CHECKLIST_PARADO));

        final Map<Long, BeneficioSummaryDTO> beneficiosPorId;
        if (beneficioIds.isEmpty()) beneficiosPorId = Map.of();
        else beneficiosPorId = beneficioService.resumosPorIds(beneficioIds, ra);
        final Map<Long, ManualSummaryDTO> manuaisPorId;
        if (manualIds.isEmpty()) manuaisPorId = Map.of();
        else manuaisPorId = manualService.resumosPorIds(manualIds);
        final Map<Long, ChecklistAlunoResponseDTO> checklistsPorId;
        if (checklistIds.isEmpty()) checklistsPorId = Map.of();
        else checklistsPorId = checklistAlunoService.cardsPorIds(checklistIds, ra);

        return pagina.map(n -> {
            BeneficioSummaryDTO beneficio = null;
            ManualSummaryDTO manual = null;
            ChecklistAlunoResponseDTO checklist = null;
            if (n.getTipo() == TipoNotificacao.BENEFICIO_NOVO) {
                beneficio = beneficiosPorId.get(n.getReferenciaId());
            } else if (n.getTipo() == TipoNotificacao.MANUAL_NOVO) {
                manual = manuaisPorId.get(n.getReferenciaId());
            } else {
                checklist = checklistsPorId.get(n.getReferenciaId());
            }
            return new NotificacaoResponseDTO(n.getId(), n.getTipo(), n.getMensagem(), n.getLida(), n.getCriadoEm(),
                    beneficio, checklist, manual);
        });
    }

    @Transactional(readOnly = true)
    public TotalNaoLidasResponseDTO totalNaoLidas(String ra) {
        return new TotalNaoLidasResponseDTO(notificacaoRepository.countByUsuarioRaAndLidaFalse(ra));
    }

    @Transactional
    public void marcarComoLida(Long id, String ra) {
        // Inexistente e de outro aluno caem no mesmo 404, para não confirmar que o id existe.
        if (notificacaoRepository.marcarComoLida(id, ra) == 0) {
            throw new ResourceNotFoundException("Notificacao nao encontrada");
        }
    }

    @Transactional
    public void marcarTodasComoLidas(String ra) {
        notificacaoRepository.marcarTodasComoLidas(ra);
    }

    /** Sempre os quatro tipos, na ordem do enum; ligado é o padrão de quem nunca mexeu. */
    @Transactional(readOnly = true)
    public List<PreferenciaNotificacaoDTO> listarPreferencias(String ra) {
        Set<String> desativados = Set.copyOf(notificacaoRepository.tiposDesativados(ra));
        return Arrays.stream(TipoNotificacao.values())
                .map(tipo -> new PreferenciaNotificacaoDTO(tipo, !desativados.contains(tipo.name())))
                .toList();
    }

    /** Idempotente: desligar o que já está desligado, ou ligar o que já está ligado, não altera nada. */
    @Transactional
    public void alterarPreferencia(String ra, TipoNotificacao tipo, boolean ativo) {
        if (ativo) {
            notificacaoRepository.ativar(ra, tipo.name());
        } else {
            notificacaoRepository.desativar(ra, tipo.name());
        }
    }

    private Set<Long> idsDaPagina(Page<Notificacao> pagina, Set<TipoNotificacao> tipos) {
        return pagina.getContent().stream()
                .filter(n -> tipos.contains(n.getTipo()))
                .map(Notificacao::getReferenciaId)
                .collect(Collectors.toSet());
    }
}
