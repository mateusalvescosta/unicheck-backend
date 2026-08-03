package com.unicheck.usuario.dto;
/**
 * DTO de ENTRADA (Request) do Admin.
 *
 * Mesma lógica do AlunoRequestDTO: usuarioId em vez do objeto Usuario inteiro.
 * A Service busca o Usuario pelo id e monta o Admin de verdade antes de salvar.
 */
public record AdminRequestDTO(
        Long usuarioId,
        String cargo
) {
}
