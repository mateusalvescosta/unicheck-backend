package com.unicheck.usuario.mapper;

import com.unicheck.usuario.domain.Usuario;
import com.unicheck.usuario.dto.UsuarioRequestDTO;
import com.unicheck.usuario.dto.UsuarioResponseDTO;
import org.mapstruct.Mapper;

/**
 * Mapper do Usuario.
 *
 * @Mapper(componentModel = "spring") -> diz pro MapStruct gerar a implementação
 * já como um @Component do Spring, então dá pra injetar esse Mapper normalmente
 * (@Autowired ou via construtor) dentro da Service, sem precisar instanciar na mão.
 *
 * O MapStruct gera a implementação em tempo de COMPILAÇÃO (não é reflection em
 * runtime tipo o ModelMapper) -- fica um arquivo "UsuarioMapperImpl.java" dentro
 * de target/generated-sources/annotations depois do "mvn compile".
 *
 * Como os nomes dos campos do DTO batem exatamente com os da entidade
 * (nome, senha, role, fotoUrl, email...), o MapStruct mapeia tudo sozinho,
 * sem precisar de @Mapping explícito.
 *
 * IMPORTANTE: toEntity() aqui gera um Usuario com "id" e "criadoEm" nulos --
 * isso é esperado, porque quem preenche id é o banco (@GeneratedValue) e
 * quem preenche criadoEm é o @PrePersist da própria entidade.
 */
@Mapper(componentModel = "spring")
public interface UsuarioMapper {

    Usuario toEntity(UsuarioRequestDTO dto);

    UsuarioResponseDTO toResponseDTO(Usuario entity);
}