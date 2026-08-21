package com.unicheck.usuario.mapper;

import com.unicheck.usuario.domain.Usuario;
import com.unicheck.usuario.dto.UsuarioRequestDTO;
import com.unicheck.usuario.dto.UsuarioResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper do Usuario.
 *
 * @Mapper(componentModel = "spring") -> diz pro MapStruct gerar a implementação
 * já como um @Component do Spring, então dá pra injetar esse Mapper normalmente
 * (@Autowired ou via construtor) dentro da Service, sem precisar instanciar na mão.
 * <p>
 * O MapStruct gera a implementação em tempo de COMPILAÇÃO (não é reflection em
 * runtime tipo o ModelMapper) -- fica um arquivo "UsuarioMapperImpl.java" dentro
 * de target/generated-sources/annotations depois do "mvn compile".
 * <p>
 * Como os nomes dos campos do DTO batem exatamente com os da entidade
 * (nome, senha, role, fotoUrl, email...), o MapStruct mapeia tudo sozinho,
 * sem precisar de @Mapping explícito.
 * <p>
 * IMPORTANTE: toEntity() aqui gera um Usuario com "id" e "criadoEm" nulos --
 * isso é esperado, porque quem preenche id é o banco (@GeneratedValue) e
 * quem preenche criadoEm é o @PrePersist da própria entidade.
 */
@Mapper(componentModel = "spring")
public interface UsuarioMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "criadoEm", ignore = true)
    @Mapping(target = "trocarSenhaNoLogin", ignore = true)
    @Mapping(target = "authorities", ignore = true)
    Usuario toEntity(UsuarioRequestDTO dto);

    UsuarioResponseDTO toResponseDTO(Usuario entity);
}
