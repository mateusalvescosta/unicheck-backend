package com.unicheck.curso.mapper;

import com.unicheck.curso.domain.Curso;
import com.unicheck.curso.dto.CursoRequestDTO;
import com.unicheck.curso.dto.CursoResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CursoMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "beneficios", ignore = true)
    Curso toEntity(CursoRequestDTO dto);

    CursoResponseDTO toDto(Curso curso);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "beneficios", ignore = true)
    void update(@MappingTarget Curso curso, CursoRequestDTO dto);
}
