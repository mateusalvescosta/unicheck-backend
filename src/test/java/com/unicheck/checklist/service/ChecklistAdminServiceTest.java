package com.unicheck.checklist.service;

import com.unicheck.notificacao.enumeration.TipoNotificacao;
import com.unicheck.notificacao.service.NotificacaoService;
import com.unicheck.checklist.domain.*;
import com.unicheck.checklist.dto.ChecklistRequestDTO;
import com.unicheck.checklist.enumeration.StatusChecklist;
import com.unicheck.checklist.repository.*;
import com.unicheck.exception.ResourceConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChecklistAdminServiceTest {
    @Mock ChecklistRepository checklistRepository; @Mock ItemChecklistRepository itemRepository;
    @Mock ProgressoItemRepository progressoRepository; @Mock ChecklistConcluidoRepository concluidoRepository;
    @Mock ChecklistPrerequisitoRepository prerequisitoRepository;
    @Mock NotificacaoService notificacaoService;
    @InjectMocks ChecklistAdminService service;

    @Test void naoPublicaChecklistVazio() {
        Checklist c = checklist(1L, StatusChecklist.RASCUNHO); when(checklistRepository.bloquearTodos()).thenReturn(List.of(c));
        when(itemRepository.findByChecklistIdOrderByOrdem(1L)).thenReturn(List.of());
        assertThrows(ResourceConflictException.class, () -> service.publicar(1L));
    }

    @Test void naoPublicaSequenciaComLacuna() {
        Checklist c = checklist(1L, StatusChecklist.RASCUNHO); ItemChecklist item = item(1L, c, 2);
        when(checklistRepository.bloquearTodos()).thenReturn(List.of(c));
        when(itemRepository.findByChecklistIdOrderByOrdem(1L)).thenReturn(List.of(item));
        assertThrows(ResourceConflictException.class, () -> service.publicar(1L));
    }

    @Test void bloqueiaMudancaEstruturalAposProgresso() {
        Checklist c = checklist(1L, StatusChecklist.PUBLICADO); when(checklistRepository.bloquearPorId(1L)).thenReturn(Optional.of(c));
        when(progressoRepository.existsByIdItemChecklistId(1L)).thenReturn(true);
        ChecklistRequestDTO alterado = new ChecklistRequestDTO("Fase", "Correcao", 2, 50, 4);
        assertThrows(ResourceConflictException.class, () -> service.atualizar(1L, alterado));
    }

    @Test void rejeitaCicloIndiretoDePrerequisitos() {
        Checklist c1 = checklist(1L, StatusChecklist.RASCUNHO); Checklist c2 = checklist(2L, StatusChecklist.PUBLICADO);
        when(checklistRepository.bloquearTodos()).thenReturn(List.of(c1, c2));
        when(prerequisitoRepository.findByIdChecklistId(2L)).thenReturn(List.of(new ChecklistPrerequisito(c2, c1)));
        assertThrows(ResourceConflictException.class, () -> service.adicionarPrerequisito(1L, 2L));
    }

    @Test void primeiraPublicacaoNotificaOsAlunos() {
        Checklist c = checklist(1L, StatusChecklist.RASCUNHO); when(checklistRepository.bloquearTodos()).thenReturn(List.of(c));
        when(itemRepository.findByChecklistIdOrderByOrdem(1L)).thenReturn(List.of(item(1L, c, 1)));
        service.publicar(1L);
        verify(notificacaoService).notificarAlunos(TipoNotificacao.CHECKLIST_NOVO, 1L, "Novo checklist: Fase");
    }

    @Test void publicarOQueJaEstaPublicadoNaoNotificaDeNovo() {
        Checklist c = checklist(1L, StatusChecklist.PUBLICADO); when(checklistRepository.bloquearTodos()).thenReturn(List.of(c));
        when(itemRepository.findByChecklistIdOrderByOrdem(1L)).thenReturn(List.of(item(1L, c, 1)));
        service.publicar(1L);
        verifyNoInteractions(notificacaoService);
    }

    @Test void publicacaoRejeitadaNaoNotifica() {
        Checklist c = checklist(1L, StatusChecklist.RASCUNHO); when(checklistRepository.bloquearTodos()).thenReturn(List.of(c));
        when(itemRepository.findByChecklistIdOrderByOrdem(1L)).thenReturn(List.of());
        assertThrows(ResourceConflictException.class, () -> service.publicar(1L));
        verifyNoInteractions(notificacaoService);
    }

    @Test void excluirRemoveAsNotificacoesDeNovoEParadoAntesDeApagar() {
        Checklist c = checklist(1L, StatusChecklist.RASCUNHO); when(checklistRepository.bloquearPorId(1L)).thenReturn(Optional.of(c));
        service.excluir(1L);
        InOrder ordem = inOrder(notificacaoService, checklistRepository);
        ordem.verify(notificacaoService).removerDoObjeto(1L, TipoNotificacao.CHECKLIST_NOVO, TipoNotificacao.CHECKLIST_PARADO);
        ordem.verify(checklistRepository).delete(c);
    }

    private Checklist checklist(Long id, StatusChecklist status) { Checklist c = new Checklist(); c.setId(id); c.setNome("Fase"); c.setDescricao("Desc"); c.setOrdem(1); c.setXp(20); c.setHorasComplementares(3); c.setStatus(status); c.setItens(new ArrayList<>()); return c; }
    private ItemChecklist item(Long id, Checklist c, int ordem) { ItemChecklist i = new ItemChecklist(); i.setId(id); i.setChecklist(c); i.setTitulo("Etapa"); i.setDescricao("Resumo"); i.setConteudo("# Conteudo"); i.setOrdem(ordem); i.setXp(10); return i; }
}
