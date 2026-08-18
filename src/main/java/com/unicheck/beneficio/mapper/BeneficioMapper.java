package com.unicheck.beneficio.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.unicheck.beneficio.domain.Beneficio;
import com.unicheck.beneficio.dto.BeneficioRequestDTO;


@Mapper(componentModel = "spring")
public interface BeneficioMapper {
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "categorias", ignore = true)
    Beneficio toEntity(BeneficioRequestDTO dto);

    BeneficioRequestDTO toDto(Beneficio entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "categorias", ignore = true)
    void update(@MappingTarget Beneficio entity, BeneficioRequestDTO dto);


    
}
