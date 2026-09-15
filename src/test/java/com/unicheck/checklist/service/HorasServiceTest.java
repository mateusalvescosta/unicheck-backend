package com.unicheck.checklist.service;

import com.unicheck.checklist.domain.*;
import com.unicheck.checklist.enumeration.StatusLancamentoHoras;
import com.unicheck.checklist.repository.ChecklistConcluidoRepository;
import com.unicheck.exception.ResourceConflictException;
import com.unicheck.usuario.domain.*;
import com.unicheck.usuario.service.IdentidadeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.OffsetDateTime;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HorasServiceTest {
    @Mock ChecklistConcluidoRepository repository; @Mock IdentidadeService identidadeService;
    @InjectMocks HorasService service;

    @Test void confirmaLancamentoSemAlterarHorasConquistadas() {
        ChecklistConcluido c = conclusao(StatusLancamentoHoras.PENDENTE); Admin admin = new Admin(); admin.setId(9L);
        when(repository.bloquear(7L, 1L)).thenReturn(Optional.of(c)); when(identidadeService.adminPorRa("ADM")).thenReturn(admin);
        var resposta = service.confirmar(7L, 1L, "ADM");
        assertEquals(4, resposta.horasConquistadas()); assertEquals(StatusLancamentoHoras.LANCADO, resposta.status());
        assertEquals(9L, resposta.adminResponsavelId()); assertNotNull(resposta.dataLancamento());
    }

    @Test void rejeitaConfirmacaoDuplicada() {
        when(repository.bloquear(7L, 1L)).thenReturn(Optional.of(conclusao(StatusLancamentoHoras.LANCADO)));
        assertThrows(ResourceConflictException.class, () -> service.confirmar(7L, 1L, "ADM"));
        verifyNoInteractions(identidadeService);
    }

    private ChecklistConcluido conclusao(StatusLancamentoHoras status) {
        Usuario u = new Usuario(); u.setNome("Aluno"); Aluno a = new Aluno(); a.setId(7L); a.setUsuario(u);
        Checklist ck = new Checklist(); ck.setId(1L); ck.setNome("Fase");
        ChecklistConcluido c = new ChecklistConcluido(a, ck, 4, OffsetDateTime.now()); c.setXpCreditado(10); c.setStatusLancamentoHoras(status);
        return c;
    }
}
