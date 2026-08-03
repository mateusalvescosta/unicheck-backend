package com.unicheck.usuario.dto;

import com.unicheck.usuario.enumeration.StatusMatricula;

/**
 * DTO de SAÍDA (Response) do Aluno.
 *
 * Aqui, diferente do Request, faz sentido devolver o UsuarioResponseDTO
 * completo (aninhado/nested) em vez de só o id -- assim o front-end já recebe
 * nome, email, fotoUrl etc. do usuário junto com os dados do aluno, sem
 * precisar fazer uma segunda chamada na API.
 *
 * O campo cursoId ficou como Long simples (não aninhado) porque ainda não
 * temos o CursoResponseDTO pronto -- quando o domínio de Curso estiver
 * modelado, dá pra trocar por um CursoResponseDTO igual fizemos com Usuario.
 */
public record AlunoResponseDTO(
        Long id,
        UsuarioResponseDTO usuario,
        Long cursoId,
        String ra,
        StatusMatricula status,
        String periodo,
        Integer xptotal
) {
}
