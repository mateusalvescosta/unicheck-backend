package com.unicheck.usuario.mapper;

import com.unicheck.usuario.domain.Aluno;
import com.unicheck.usuario.dto.AlunoRequestDTO;
import com.unicheck.usuario.dto.AlunoResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper do Aluno.
 *
 * uses = UsuarioMapper.class -> avisa o MapStruct "se precisar converter um
 * Usuario pra dentro desse mapper, usa o UsuarioMapper que já existe" --
 * é assim que o campo "usuario" (aninhado) do AlunoResponseDTO é preenchido
 * automaticamente com nome/email/etc, sem repetir código.
 *
 * toEntity(AlunoRequestDTO):
 * O DTO de request só tem usuarioId e cursoId (Long), não os objetos
 * Usuario/Curso completos -- e o MapStruct NÃO sabe ir no banco buscar essas
 * entidades sozinho (ele só faz "cópia de campos", não faz query).
 * Por isso ignoramos os dois aqui com @Mapping(ignore = true) e quem
 * de fato busca no repository e seta esses campos é a Service, assim:
 *
 *   Aluno aluno = alunoMapper.toEntity(dto);
 *   aluno.setUsuario(usuarioRepository.findById(dto.usuarioId())...);
 *   aluno.setCurso(cursoRepository.findById(dto.cursoId())...);
 *
 * toResponseDTO(Aluno):
 * Aqui é o caminho inverso, e nesse sentido o MapStruct SIM consegue ajudar,
 * porque ele só está lendo campos que já existem na entidade (entity.getCurso().getId()),
 * não precisa consultar nada novo no banco.
 */
@Mapper(componentModel = "spring", uses = UsuarioMapper.class)
public interface AlunoMapper {

    @Mapping(target = "usuario", ignore = true)
    @Mapping(target = "curso", ignore = true)
    Aluno toEntity(AlunoRequestDTO dto);

    @Mapping(source = "curso.id", target = "cursoId")
    AlunoResponseDTO toResponseDTO(Aluno entity);
}