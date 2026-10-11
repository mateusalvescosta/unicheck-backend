package com.unicheck.notificacao.service;

import com.unicheck.notificacao.enumeration.TipoNotificacao;
import com.unicheck.notificacao.repository.NotificacaoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * @apiNote O NotificacaoService só repassa ao repository; aqui fica garantido o que ele repassa.
 *          As regras de quem recebe e de quando um checklist está parado estão no SQL e são
 *          testadas contra o Postgres em NotificacaoRepositoryTest.
 */
@ExtendWith(MockitoExtension.class)
class NotificacaoServiceTest {
    @Mock private NotificacaoRepository notificacaoRepository;
    @InjectMocks private NotificacaoService service;

    @Test
    void notificarAlunosPassaOTipoPeloNomeDoEnum() {
        service.notificarAlunos(TipoNotificacao.BENEFICIO_NOVO, 7L, "Novo benefício: Bolsa");

        // A query é nativa e a coluna guarda o nome do enum.
        verify(notificacaoRepository).notificarAlunos("BENEFICIO_NOVO", 7L, "Novo benefício: Bolsa");
    }

    @Test
    void removerDoObjetoRemoveTodosOsTiposInformados() {
        service.removerDoObjeto(3L, TipoNotificacao.CHECKLIST_NOVO, TipoNotificacao.CHECKLIST_PARADO);

        verify(notificacaoRepository).removerDoObjeto(
                List.of(TipoNotificacao.CHECKLIST_NOVO, TipoNotificacao.CHECKLIST_PARADO), 3L);
    }

    @Test
    void checklistsParadosUsaOInstanteRecebidoEOsDiasConfigurados() {
        ReflectionTestUtils.setField(service, "diasParaPrimeiro", 3);
        ReflectionTestUtils.setField(service, "diasEntreLembretes", 7);
        LocalDateTime agora = LocalDateTime.of(2026, 10, 11, 9, 0);
        when(notificacaoRepository.notificarChecklistsParados(agora, 3, 7)).thenReturn(2);

        assertEquals(2, service.notificarChecklistsParados(agora));
    }

    @Test
    void apagarAntigasUsaOsPrazosConfiguradosDeLidasENaoLidas() {
        ReflectionTestUtils.setField(service, "diasRetencaoLidas", 30);
        ReflectionTestUtils.setField(service, "diasRetencaoNaoLidas", 90);
        LocalDateTime agora = LocalDateTime.of(2026, 12, 1, 3, 0);
        when(notificacaoRepository.apagarAntigas(agora, 30, 90)).thenReturn(5);

        assertEquals(5, service.apagarAntigas(agora));
    }
}
