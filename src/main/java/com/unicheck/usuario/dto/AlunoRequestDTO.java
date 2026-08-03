package com.unicheck.usuario.dto;

import com.unicheck.usuario.enumeration.StatusMatricula;

/**
 * DTO de ENTRADA (Request) do Aluno.
 *
 * ATENÇÃO com os relacionamentos (usuarioId e cursoId):
 * A entidade Aluno tem "Usuario usuario" e "Curso curso" como OBJETOS
 * (@OneToOne e @ManyToOne). Só que no DTO de request a gente NUNCA manda
 * o objeto inteiro, só o ID de cada um -- é o cliente dizendo
 * "esse aluno pertence ao usuário X e ao curso Y".
 *
 * Quem transforma esse "usuarioId" no objeto "Usuario" de verdade é a
 * Service (usando o UsuarioRepository.findById(usuarioId) e o
 * CursoRepository.findById(cursoId)) -- o Mapper sozinho NÃO consegue fazer
 * essa busca no banco, então essa parte do "de-para" fica de fora do
 * AlunoMapper (vê o comentário lá no mapper).
 */
public record AlunoRequestDTO(
        Long usuarioId,
        Long cursoId,
        String ra,
        StatusMatricula status,
        String periodo
) {
}