package com.unicheck.gameficacao.service;

import com.unicheck.gameficacao.domain.*;
import com.unicheck.gameficacao.repository.*;
import com.unicheck.usuario.domain.Aluno;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.OffsetDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameficacaoServiceTest {
    @Mock NivelRepository nivelRepository; @Mock AlunoSeloRepository alunoSeloRepository;
    @InjectMocks
    GameficacaoService service;

    @Test void creditaXpMesmoSemCatalogoDeNiveis() {
        Aluno aluno = aluno(100); when(nivelRepository.findByXpMinimoLessThanEqualOrderByXpMinimoAsc(125)).thenReturn(List.of());
        service.concederXp(aluno, 25, OffsetDateTime.now());
        assertEquals(125, aluno.getXpTotal()); verifyNoInteractions(alunoSeloRepository);
    }

    @Test void ganhoPodeConquistarMaisDeUmSeloSemDuplicarExistente() {
        Aluno aluno = aluno(0); Nivel n1 = nivel(1L, 0, selo(11L)); Nivel n2 = nivel(2L, 100, selo(12L));
        when(nivelRepository.findByXpMinimoLessThanEqualOrderByXpMinimoAsc(150)).thenReturn(List.of(n1, n2));
        when(alunoSeloRepository.existsByIdAlunoIdAndIdSeloId(7L, 11L)).thenReturn(true);
        when(alunoSeloRepository.existsByIdAlunoIdAndIdSeloId(7L, 12L)).thenReturn(false);
        service.concederXp(aluno, 150, OffsetDateTime.now());
        verify(alunoSeloRepository, times(1)).save(any(AlunoSelo.class));
    }

    private Aluno aluno(int xp) { Aluno a = new Aluno(); a.setId(7L); a.setXpTotal(xp); return a; }
    private Selo selo(Long id) { Selo s = new Selo(); s.setId(id); s.setNome("Selo" + id); return s; }
    private Nivel nivel(Long id, int xp, Selo selo) { Nivel n = new Nivel(); n.setId(id); n.setNome("Nivel" + id); n.setXpMinimo(xp); n.setSelo(selo); return n; }
}
