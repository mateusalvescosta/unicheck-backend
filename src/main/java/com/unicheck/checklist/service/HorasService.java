package com.unicheck.checklist.service;

import com.unicheck.checklist.domain.ChecklistConcluido;
import com.unicheck.checklist.dto.*;
import com.unicheck.checklist.enumeration.StatusLancamentoHoras;
import com.unicheck.checklist.repository.ChecklistConcluidoRepository;
import com.unicheck.exception.*;
import com.unicheck.usuario.domain.*;
import com.unicheck.usuario.service.IdentidadeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/** Separa horas conquistadas na conclusão do registro posterior de lançamento pela faculdade. */
@Service @RequiredArgsConstructor
public class HorasService {
    private final ChecklistConcluidoRepository repository;
    private final IdentidadeService identidadeService;

    @Transactional(readOnly = true)
    public ResumoHorasAlunoDTO minhasHoras(String ra) {
        Aluno aluno = identidadeService.alunoPorRa(ra);
        List<HorasResponseDTO> registros = repository.findByIdAlunoIdOrderByDataConclusaoDesc(aluno.getId()).stream().map(this::dto).toList();
        int conquistadas = registros.stream().mapToInt(r -> nvl(r.horasConquistadas())).sum();
        int pendentes = registros.stream().filter(r -> r.status() == StatusLancamentoHoras.PENDENTE).mapToInt(r -> nvl(r.horasConquistadas())).sum();
        int lancadas = registros.stream().filter(r -> r.status() == StatusLancamentoHoras.LANCADO).mapToInt(r -> nvl(r.horasConquistadas())).sum();
        return new ResumoHorasAlunoDTO(conquistadas, pendentes, lancadas, registros);
    }

    @Transactional(readOnly = true)
    public List<HorasResponseDTO> listar(StatusLancamentoHoras status) {
        List<ChecklistConcluido> registros = status == null ? repository.findAllByOrderByDataConclusaoDesc()
                : repository.findByStatusLancamentoHorasOrderByDataConclusaoAsc(status);
        return registros.stream().map(this::dto).toList();
    }

    @Transactional
    public HorasResponseDTO confirmar(Long alunoId, Long checklistId, String raAdmin) {
        ChecklistConcluido registro = repository.bloquear(alunoId, checklistId)
                .orElseThrow(() -> new ResourceNotFoundException("Conclusao de checklist nao encontrada"));
        if (registro.getStatusLancamentoHoras() != StatusLancamentoHoras.PENDENTE)
            throw new ResourceConflictException("Horas nao estao pendentes ou ja foram lancadas");
        Admin admin = identidadeService.adminPorRa(raAdmin);
        // Horas já foram conquistadas na conclusão; esta operação apenas registra o lançamento institucional integral.
        registro.setStatusLancamentoHoras(StatusLancamentoHoras.LANCADO);
        registro.setDataLancamentoHoras(OffsetDateTime.now());
        registro.setAdminLancamento(admin);
        return dto(registro);
    }

    private HorasResponseDTO dto(ChecklistConcluido c) {
        return new HorasResponseDTO(c.getAluno().getId(), c.getAluno().getUsuario().getNome(), c.getChecklist().getId(),
                c.getChecklist().getNome(), c.getHorasCreditadas(), c.getStatusLancamentoHoras(), c.getDataConclusao(),
                c.getDataLancamentoHoras(), c.getAdminLancamento() == null ? null : c.getAdminLancamento().getId());
    }
    private int nvl(Integer valor) { return valor == null ? 0 : valor; }
}
