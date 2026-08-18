package com.unicheck.beneficio.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.unicheck.beneficio.domain.Beneficio;
import com.unicheck.beneficio.dto.BeneficioRequestDTO;
import com.unicheck.beneficio.dto.BeneficioSummaryDTO;
import com.unicheck.beneficio.dto.BeneficioResponseDTO;


@Mapper(componentModel = "spring")
public interface BeneficioMapper {
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "categorias", ignore = true)
    Beneficio toEntity(BeneficioRequestDTO dto);

    BeneficioResponseDTO toDto(Beneficio entity);
    BeneficioSummaryDTO toDtoSummary(Beneficio entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "categorias", ignore = true)
    void update(@MappingTarget Beneficio entity, BeneficioRequestDTO dto);
}
