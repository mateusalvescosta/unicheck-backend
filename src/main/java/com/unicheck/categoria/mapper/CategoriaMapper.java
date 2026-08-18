package com.unicheck.categoria.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.unicheck.categoria.domain.Categoria;
import com.unicheck.categoria.dto.CategoriaRequestDTO;
import com.unicheck.categoria.dto.CategoriaResponseDTO;

@Mapper(componentModel = "spring")
public interface CategoriaMapper {

    CategoriaResponseDTO toDto(Categoria entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "beneficios", ignore = true)
    Categoria toEntity(CategoriaRequestDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "beneficios", ignore = true)
    void update(@MappingTarget Categoria entity, CategoriaRequestDTO dto);
    
}
