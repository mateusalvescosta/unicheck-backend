package com.unicheck.usuario.dto;

import com.unicheck.usuario.enumeration.StatusMatricula;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO de ENTRADA (Request) do Aluno.
 *
 * ATENÇÃO com o relacionamento (usuarioId):
 * A entidade Aluno tem "Usuario usuario" como OBJETO (@OneToOne). Só que
 * no DTO de request a gente NUNCA manda o objeto inteiro, só o ID -- é o
 * cliente dizendo "esse aluno pertence ao usuário X".
 *
 * Quem transforma esse "usuarioId" no objeto "Usuario" de verdade é a
 * Service (usando o UsuarioRepository.findById(usuarioId)) -- o Mapper
 * sozinho NÃO consegue fazer essa busca no banco, então essa parte do
 * "de-para" fica de fora do AlunoMapper (vê o comentário lá no mapper).
 */
public record AlunoRequestDTO(
        @NotNull(message = "Usuário é obrigatório!")
        Long usuarioId,

        @NotNull(message = "Status de matrícula é obrigatório!")
        StatusMatricula status,

        @NotBlank(message = "Período é obrigatório!")
        String periodo
) {
}