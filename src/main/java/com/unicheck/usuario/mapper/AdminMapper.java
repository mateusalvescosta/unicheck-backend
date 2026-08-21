package com.unicheck.usuario.mapper;

import com.unicheck.usuario.domain.Admin;
import com.unicheck.usuario.dto.AdminResponseDTO;
import com.unicheck.usuario.dto.CriarAdminRequestDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper do Admin.
 * <p>
 * toEntity() agora recebe CriarAdminRequestDTO
 * <p>
 * "usuario" continua ignorado aqui pelo mesmo motivo de sempre: o DTO só
 * tem dados soltos (nome, email, senha, cargo), não um usuarioId --
 * a Service que cria o Usuario primeiro e depois seta ele no Admin.
 * <p>
 * "cargo" mapeia sozinho, já que o nome bate igual nos dois lados.
 */
@Mapper(componentModel = "spring", uses = UsuarioMapper.class)
public interface AdminMapper {

    @Mapping(target = "usuario", ignore = true)
    @Mapping(target = "id", ignore = true)
    Admin toEntity(CriarAdminRequestDTO dto);

    AdminResponseDTO toResponseDTO(Admin entity);
}
