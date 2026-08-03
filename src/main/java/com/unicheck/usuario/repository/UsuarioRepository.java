package com.unicheck.usuario.repository;

import com.unicheck.usuario.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository do Usuario.
 *
 * Só precisa estender JpaRepository<Entidade, TipoDoId> que o Spring Data JPA
 * já gera a implementação sozinho em tempo de execução (não precisa @Repository,
 * mas pode colocar se quiser deixar explícito).
 *
 * JpaRepository<Usuario, Long> -> Long porque o id do Usuario é Long (@GeneratedValue IDENTITY).
 *
 * Métodos como save(), findById(), findAll(), deleteById() já vêm de graça.
 * Os métodos abaixo são "query methods": o Spring lê o nome do método e monta
 * a query JPQL sozinho, sem precisar escrever SQL/JPQL na mão.
 */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Usado no login: busca o usuário pelo email (que é @Column(unique = true)).
    Optional<Usuario> findByEmail(String email);

    // Útil pra validar duplicidade antes de salvar um novo usuário
    // (ex: "esse email já existe?").
    boolean existsByEmail(String email);
}
