package com.unicheck.usuario.mapper;

import com.unicheck.usuario.domain.Aluno;
import com.unicheck.usuario.dto.AlunoRequestDTO;
import com.unicheck.usuario.dto.AlunoResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper do Aluno.
 * <p>
 * uses = UsuarioMapper.class -> avisa o MapStruct "se precisar converter um
 * Usuario pra dentro desse mapper, usa o UsuarioMapper que já existe" --
 * é assim que o campo "usuario" (aninhado) do AlunoResponseDTO é preenchido
 * automaticamente com nome/email/etc, sem repetir código.
 * <p>
 * toEntity(AlunoRequestDTO):
 * O DTO de request só tem usuarioId (Long), não o objeto Usuario completo --
 * e o MapStruct NÃO sabe ir no banco buscar essa entidade sozinho (ele só
 * faz "cópia de campos", não faz query). Por isso ignoramos aqui com
 * @Mapping(ignore = true) e quem de fato busca no repository e seta esse
 * campo é a Service, assim:
 * <p>
 * Aluno aluno = alunoMapper.toEntity(dto);
 * aluno.setUsuario(usuarioRepository.findById(dto.usuarioId())...);
 * <p>
 * toResponseDTO(Aluno):
 * Aqui é o caminho inverso, e nesse sentido o MapStruct SIM consegue ajudar,
 * porque ele só está lendo campos que já existem na entidade (entity.getUsuario()...),
 * não precisa consultar nada novo no banco.
 */
@Mapper(componentModel = "spring", uses = UsuarioMapper.class)
public interface AlunoMapper {

    @Mapping(target = "usuario", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "xpTotal", ignore = true)
    @Mapping(target = "progressos", ignore = true)
    @Mapping(target = "concluidos", ignore = true)
    Aluno toEntity(AlunoRequestDTO dto);

    AlunoResponseDTO toResponseDTO(Aluno entity);
}
