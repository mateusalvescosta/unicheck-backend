package com.unicheck.gameficacao.dto;
import java.time.OffsetDateTime;
import java.util.List;
public record GameficacaoResponseDTO(Integer xpTotal, NivelDTO nivelAtual, List<SeloDTO> selos) {
    public record NivelDTO(Long id, String nome, Integer xpMinimo) {}
    public record SeloDTO(Long id, String nome, String descricao, String imagemUrl, OffsetDateTime dataConquista) {}
}
