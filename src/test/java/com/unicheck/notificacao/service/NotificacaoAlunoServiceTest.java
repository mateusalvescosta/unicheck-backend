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
import com.unicheck.notificacao.enumeration.TipoNotificacao;
import com.unicheck.notificacao.repository.NotificacaoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * @apiNote Leitura das notificações do aluno: montagem do objeto de cada tipo, contador,
 *          marcar como lida e preferências.
 */
@ExtendWith(MockitoExtension.class)
class NotificacaoAlunoServiceTest {
    private static final PageRequest PAGINA = PageRequest.of(0, 20);

    @Mock private NotificacaoRepository notificacaoRepository;
    @Mock private BeneficioService beneficioService;
    @Mock private ManualService manualService;
    @Mock private ChecklistAlunoService checklistAlunoService;
    @InjectMocks private NotificacaoAlunoService service;

    @Test
    void listarPreencheSoOObjetoDoTipoDeCadaNotificacao() {
        when(notificacaoRepository.findByUsuarioRaOrderByCriadoEmDescIdDesc("RA001", PAGINA)).thenReturn(pagina(
                notificacao(1L, TipoNotificacao.BENEFICIO_NOVO, 10L),
                notificacao(2L, TipoNotificacao.MANUAL_NOVO, 20L),
                notificacao(3L, TipoNotificacao.CHECKLIST_PARADO, 30L)));
        BeneficioSummaryDTO beneficio = mock(BeneficioSummaryDTO.class);
        ManualSummaryDTO manual = mock(ManualSummaryDTO.class);
        ChecklistAlunoResponseDTO checklist = mock(ChecklistAlunoResponseDTO.class);
        when(beneficioService.resumosPorIds(Set.of(10L), "RA001")).thenReturn(Map.of(10L, beneficio));
        when(manualService.resumosPorIds(Set.of(20L))).thenReturn(Map.of(20L, manual));
        when(checklistAlunoService.cardsPorIds(Set.of(30L), "RA001")).thenReturn(Map.of(30L, checklist));

        List<NotificacaoResponseDTO> lista = service.listar("RA001", null, PAGINA).getContent();

        assertSame(beneficio, lista.get(0).beneficio());
        assertNull(lista.get(0).manual());
        assertNull(lista.get(0).checklist());
        assertSame(manual, lista.get(1).manual());
        assertNull(lista.get(1).beneficio());
        assertSame(checklist, lista.get(2).checklist());
        assertNull(lista.get(2).beneficio());
    }

    @Test
    void listarBuscaCadaTipoUmaVezSoPorPagina() {
        when(notificacaoRepository.findByUsuarioRaOrderByCriadoEmDescIdDesc("RA001", PAGINA)).thenReturn(pagina(
                notificacao(1L, TipoNotificacao.CHECKLIST_NOVO, 30L),
                notificacao(2L, TipoNotificacao.CHECKLIST_PARADO, 30L),
                notificacao(3L, TipoNotificacao.CHECKLIST_NOVO, 31L)));
        when(checklistAlunoService.cardsPorIds(any(), anyString())).thenReturn(Map.of());

        service.listar("RA001", null, PAGINA);

        // Os dois tipos de checklist vão juntos num lote só, sem repetir o id 30.
        verify(checklistAlunoService, times(1)).cardsPorIds(Set.of(30L, 31L), "RA001");
        // Tipos ausentes da página não disparam busca nenhuma.
        verifyNoInteractions(beneficioService, manualService);
    }

    @Test
    void listarComFiltroDeLidaUsaAConsultaFiltrada() {
        when(notificacaoRepository.findByUsuarioRaAndLidaOrderByCriadoEmDescIdDesc("RA001", false, PAGINA))
                .thenReturn(Page.empty());

        service.listar("RA001", false, PAGINA);

        verify(notificacaoRepository, never()).findByUsuarioRaOrderByCriadoEmDescIdDesc(any(), any());
    }

    @Test
    void objetoQueNaoExisteMaisVemNuloSemErro() {
        when(notificacaoRepository.findByUsuarioRaOrderByCriadoEmDescIdDesc("RA001", PAGINA))
                .thenReturn(pagina(notificacao(1L, TipoNotificacao.MANUAL_NOVO, 99L)));
        when(manualService.resumosPorIds(Set.of(99L))).thenReturn(Map.of());

        NotificacaoResponseDTO unica = service.listar("RA001", null, PAGINA).getContent().get(0);

        assertNull(unica.manual());
        assertEquals("mensagem 1", unica.mensagem());
    }

    @Test
    void totalNaoLidasVemDoRepository() {
        when(notificacaoRepository.countByUsuarioRaAndLidaFalse("RA001")).thenReturn(4L);

        assertEquals(4L, service.totalNaoLidas("RA001").total());
    }

    @Test
    void marcarComoLidaDeNotificacaoQueNaoEDoAlunoRetorna404() {
        // Zero linhas: não existe ou é de outro aluno; os dois casos ficam iguais para quem chama.
        when(notificacaoRepository.marcarComoLida(5L, "RA001")).thenReturn(0);

        assertThrows(ResourceNotFoundException.class, () -> service.marcarComoLida(5L, "RA001"));
    }

    @Test
    void marcarComoLidaDaPropriaNotificacaoNaoLancaErro() {
        when(notificacaoRepository.marcarComoLida(5L, "RA001")).thenReturn(1);

        assertDoesNotThrow(() -> service.marcarComoLida(5L, "RA001"));
    }

    @Test
    void preferenciasTrazemOsQuatroTiposComOsDesligadosFalsos() {
        when(notificacaoRepository.tiposDesativados("RA001")).thenReturn(List.of("MANUAL_NOVO"));

        List<PreferenciaNotificacaoDTO> preferencias = service.listarPreferencias("RA001");

        assertEquals(List.of(
                new PreferenciaNotificacaoDTO(TipoNotificacao.BENEFICIO_NOVO, true),
                new PreferenciaNotificacaoDTO(TipoNotificacao.CHECKLIST_NOVO, true),
                new PreferenciaNotificacaoDTO(TipoNotificacao.MANUAL_NOVO, false),
                new PreferenciaNotificacaoDTO(TipoNotificacao.CHECKLIST_PARADO, true)), preferencias);
    }

    @Test
    void alterarPreferenciaDesligaELiga() {
        service.alterarPreferencia("RA001", TipoNotificacao.MANUAL_NOVO, false);
        service.alterarPreferencia("RA001", TipoNotificacao.BENEFICIO_NOVO, true);

        verify(notificacaoRepository).desativar("RA001", "MANUAL_NOVO");
        verify(notificacaoRepository).ativar("RA001", "BENEFICIO_NOVO");
    }

    private Page<Notificacao> pagina(Notificacao... notificacoes) {
        return new PageImpl<>(List.of(notificacoes), PAGINA, notificacoes.length);
    }

    private Notificacao notificacao(Long id, TipoNotificacao tipo, Long referenciaId) {
        Notificacao n = new Notificacao();
        n.setId(id);
        n.setTipo(tipo);
        n.setReferenciaId(referenciaId);
        n.setMensagem("mensagem " + id);
        n.setLida(false);
        n.setCriadoEm(OffsetDateTime.now());
        return n;
    }
}
