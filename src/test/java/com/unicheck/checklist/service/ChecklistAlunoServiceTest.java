package com.unicheck.checklist.service;

import com.unicheck.checklist.domain.*;
import com.unicheck.checklist.dto.ConclusaoEtapaResponseDTO;
import com.unicheck.checklist.enumeration.*;
import com.unicheck.checklist.repository.*;
import com.unicheck.exception.ResourceConflictException;
import com.unicheck.gameficacao.service.GameficacaoService;
import com.unicheck.usuario.domain.Aluno;
import com.unicheck.usuario.service.IdentidadeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.OffsetDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChecklistAlunoServiceTest {
    @Mock ChecklistRepository checklistRepository;
    @Mock ItemChecklistRepository itemRepository;
    @Mock ProgressoItemRepository progressoRepository;
    @Mock ChecklistConcluidoRepository concluidoRepository;
    @Mock ChecklistPrerequisitoRepository prerequisitoRepository;
    @Mock IdentidadeService identidadeService;
    @Mock
    GameficacaoService gameficacaoService;
    @InjectMocks ChecklistAlunoService service;

    @Test
    void rejeitaConclusaoForaDaOrdem() {
        Cenario c = cenarioDuasEtapas(); preparar(c);
        when(itemRepository.findById(c.segundo.getId())).thenReturn(Optional.of(c.segundo));
        when(progressoRepository.findByIdAlunoIdAndIdItemId(c.aluno.getId(), c.segundo.getId())).thenReturn(Optional.empty());
        when(itemRepository.findByChecklistIdOrderByOrdem(c.checklist.getId())).thenReturn(List.of(c.primeiro, c.segundo));
        when(progressoRepository.findByIdAlunoIdAndIdItemChecklistId(c.aluno.getId(), c.checklist.getId()))
                .thenReturn(List.of());

        assertThrows(ResourceConflictException.class, () -> service.concluir(c.checklist.getId(), c.segundo.getId(), "RA1"));
        verifyNoInteractions(gameficacaoService);
    }

    @Test
    void repetirConclusaoNaoCreditaXpNovamente() {
        Cenario c = cenarioDuasEtapas(); preparar(c);
        ProgressoItem feito = new ProgressoItem(c.aluno, c.primeiro, StatusProgressoItem.CONCLUIDO, OffsetDateTime.now());
        feito.setXpCreditado(10);
        when(itemRepository.findById(c.primeiro.getId())).thenReturn(Optional.of(c.primeiro));
        when(progressoRepository.findByIdAlunoIdAndIdItemId(c.aluno.getId(), c.primeiro.getId())).thenReturn(Optional.of(feito));
        when(concluidoRepository.bloquear(c.aluno.getId(), c.checklist.getId())).thenReturn(Optional.empty());

        ConclusaoEtapaResponseDTO resposta = service.concluir(c.checklist.getId(), c.primeiro.getId(), "RA1");

        assertTrue(resposta.repetida());
        assertEquals(10, resposta.xpEtapaCreditado());
        verifyNoInteractions(gameficacaoService);
        verify(progressoRepository, never()).save(any());
    }

    @Test
    void ultimaEtapaCreditaEtapaBonusEHorasUmaVez() {
        Cenario c = cenarioUmaEtapa(); preparar(c);
        when(itemRepository.findById(c.primeiro.getId())).thenReturn(Optional.of(c.primeiro));
        when(progressoRepository.findByIdAlunoIdAndIdItemId(c.aluno.getId(), c.primeiro.getId())).thenReturn(Optional.empty());
        when(progressoRepository.findByIdAlunoIdAndIdItemChecklistId(c.aluno.getId(), c.checklist.getId()))
                .thenReturn(List.of());
        when(itemRepository.findByChecklistIdOrderByOrdem(c.checklist.getId())).thenReturn(List.of(c.primeiro));
        when(concluidoRepository.bloquear(c.aluno.getId(), c.checklist.getId())).thenReturn(Optional.empty());

        ConclusaoEtapaResponseDTO resposta = service.concluir(c.checklist.getId(), c.primeiro.getId(), "RA1");

        assertTrue(resposta.checklistConcluido());
        assertEquals(10, resposta.xpEtapaCreditado());
        assertEquals(20, resposta.bonusFinalCreditado());
        assertEquals(3, resposta.horasConquistadas());
        verify(gameficacaoService).concederXp(eq(c.aluno), eq(10), any());
        verify(gameficacaoService).concederXp(eq(c.aluno), eq(20), any());
        ArgumentCaptor<ChecklistConcluido> captor = ArgumentCaptor.forClass(ChecklistConcluido.class);
        verify(concluidoRepository).save(captor.capture());
        assertEquals(StatusLancamentoHoras.PENDENTE, captor.getValue().getStatusLancamentoHoras());
    }

    @Test
    void alunoNovoNaoIniciaChecklistArquivado() {
        Cenario c = cenarioUmaEtapa(); c.checklist.setStatus(StatusChecklist.ARQUIVADO);
        when(checklistRepository.bloquearPorId(c.checklist.getId())).thenReturn(Optional.of(c.checklist));
        when(identidadeService.bloquearAlunoPorRa("RA1")).thenReturn(c.aluno);
        when(progressoRepository.existsByIdAlunoIdAndIdItemChecklistId(c.aluno.getId(), c.checklist.getId())).thenReturn(false);
        when(concluidoRepository.existsByIdAlunoIdAndIdChecklistId(c.aluno.getId(), c.checklist.getId())).thenReturn(false);

        assertThrows(ResourceConflictException.class, () -> service.concluir(c.checklist.getId(), c.primeiro.getId(), "RA1"));
    }

    @Test
    void prerequisitoPendenteBloqueiaConclusao() {
        Cenario c = cenarioUmaEtapa(); preparar(c);
        Checklist requisito = checklist(2L);
        when(prerequisitoRepository.findByIdChecklistId(c.checklist.getId()))
                .thenReturn(List.of(new ChecklistPrerequisito(c.checklist, requisito)));
        when(concluidoRepository.existsByIdAlunoIdAndIdChecklistId(c.aluno.getId(), requisito.getId())).thenReturn(false);

        assertThrows(ResourceConflictException.class, () -> service.concluir(c.checklist.getId(), c.primeiro.getId(), "RA1"));
        verifyNoInteractions(gameficacaoService);
    }

    private void preparar(Cenario c) {
        when(checklistRepository.bloquearPorId(c.checklist.getId())).thenReturn(Optional.of(c.checklist));
        when(identidadeService.bloquearAlunoPorRa("RA1")).thenReturn(c.aluno);
        when(prerequisitoRepository.findByIdChecklistId(c.checklist.getId())).thenReturn(List.of());
    }
    private Cenario cenarioUmaEtapa() {
        Checklist checklist = checklist(1L); ItemChecklist primeiro = item(11L, checklist, 1, 10);
        checklist.setItens(new ArrayList<>(List.of(primeiro))); Aluno aluno = new Aluno(); aluno.setId(7L); aluno.setXpTotal(0);
        return new Cenario(checklist, primeiro, null, aluno);
    }
    private Cenario cenarioDuasEtapas() {
        Cenario c = cenarioUmaEtapa(); ItemChecklist segundo = item(12L, c.checklist, 2, 15);
        c.checklist.getItens().add(segundo); return new Cenario(c.checklist, c.primeiro, segundo, c.aluno);
    }
    private Checklist checklist(Long id) { Checklist c = new Checklist(); c.setId(id); c.setNome("Fase"); c.setDescricao("Desc"); c.setOrdem(1); c.setXp(20); c.setHorasComplementares(3); c.setStatus(StatusChecklist.PUBLICADO); return c; }
    private ItemChecklist item(Long id, Checklist c, int ordem, int xp) { ItemChecklist i = new ItemChecklist(); i.setId(id); i.setChecklist(c); i.setTitulo("Etapa"); i.setDescricao("Resumo"); i.setConteudo("# Conteudo"); i.setOrdem(ordem); i.setXp(xp); return i; }
    private record Cenario(Checklist checklist, ItemChecklist primeiro, ItemChecklist segundo, Aluno aluno) {}
}
