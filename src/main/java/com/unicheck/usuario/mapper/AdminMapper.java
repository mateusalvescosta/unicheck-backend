package com.unicheck.usuario.mapper;

import com.unicheck.usuario.domain.Admin;
import com.unicheck.usuario.dto.AdminRequestDTO;
import com.unicheck.usuario.dto.AdminResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper do Admin.
 *
 * Mesma lógica do AlunoMapper: uses = UsuarioMapper.class pra preencher o
 * campo "usuario" aninhado no response automaticamente, e ignoramos o
 * campo "usuario" no toEntity() porque o request só manda o usuarioId --
 * quem busca o Usuario de verdade e seta no Admin é a Service.
 */
@Mapper(componentModel = "spring", uses = UsuarioMapper.class)
public interface AdminMapper {

    @Mapping(target = "usuario", ignore = true)
    Admin toEntity(AdminRequestDTO dto);

    AdminResponseDTO toResponseDTO(Admin entity);
}