package com.unicheck.gameficacao.service;

import com.unicheck.gameficacao.domain.*;
import com.unicheck.gameficacao.dto.GameficacaoResponseDTO;
import com.unicheck.gameficacao.repository.*;
import com.unicheck.usuario.domain.Aluno;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

/** Usa Aluno.xpTotal como saldo único e concede os selos das faixas efetivamente atingidas. */
@Service @RequiredArgsConstructor
public class GameficacaoService {
    private final NivelRepository nivelRepository;
    private final AlunoSeloRepository alunoSeloRepository;

    /** Chamado dentro da transação que mantém o Aluno bloqueado, evitando perda de XP concorrente. */
    public void concederXp(Aluno aluno, int xp, OffsetDateTime agora) {
        aluno.setXpTotal(aluno.getXpTotal() + xp);
        // Um único ganho pode atravessar várias faixas; cada selo é protegido também pela PK do banco.
        for (Nivel nivel : nivelRepository.findByXpMinimoLessThanEqualOrderByXpMinimoAsc(aluno.getXpTotal())) {
            if (!alunoSeloRepository.existsByIdAlunoIdAndIdSeloId(aluno.getId(), nivel.getSelo().getId())) {
                alunoSeloRepository.save(new AlunoSelo(aluno, nivel.getSelo(), agora));
            }
        }
        // Catálogo vazio é válido: o XP continua sendo creditado, sem fabricar nível ou selo.
    }

    public GameficacaoResponseDTO consultar(Aluno aluno) {
        List<Nivel> niveis = nivelRepository.findByXpMinimoLessThanEqualOrderByXpMinimoAsc(aluno.getXpTotal());
        Nivel atual = niveis.isEmpty() ? null : niveis.getLast();
        var nivelDto = atual == null ? null : new GameficacaoResponseDTO.NivelDTO(atual.getId(), atual.getNome(), atual.getXpMinimo());
        var selos = alunoSeloRepository.findByIdAlunoIdOrderByDataConquista(aluno.getId()).stream()
                .map(as -> new GameficacaoResponseDTO.SeloDTO(as.getSelo().getId(), as.getSelo().getNome(),
                        as.getSelo().getDescricao(), as.getSelo().getImagemUrl(), as.getDataConquista())).toList();
        return new GameficacaoResponseDTO(aluno.getXpTotal(), nivelDto, selos);
    }
}
