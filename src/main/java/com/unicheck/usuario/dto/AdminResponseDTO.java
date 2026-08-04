package com.unicheck.usuario.dto;

/**
 * DTO de SAÍDA (Response) do Admin.
 *
 * Igual ao AlunoResponseDTO: devolve o UsuarioResponseDTO aninhado,
 * já que faz sentido o front-end ter nome/email/foto do admin junto.
 */
public record AdminResponseDTO(
        Long id,
        UsuarioResponseDTO usuario,
        String cargo
) {
}