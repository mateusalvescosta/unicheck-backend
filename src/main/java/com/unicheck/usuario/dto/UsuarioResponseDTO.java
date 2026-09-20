package com.unicheck.usuario.dto;

import com.unicheck.usuario.enumeration.RoleUsuario;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * DTO de SAÍDA (Response) do Usuario.
 *
 * É o que a API devolve pro cliente. Repare que NÃO tem o campo "senha" --
 * isso é de propósito. Nunca devolvemos senha (nem o hash dela) numa resposta
 * de API, é falha de segurança básica.
 *
 * Tem "id" e "criadoEm" aqui (que não existem no Request) porque esses campos
 * são gerados pelo banco/sistema, e só fazem sentido quando o Usuario já existe.
 */
public record UsuarioResponseDTO(
        Long id,
        String nome,
        String ra,
        LocalDate dataNascimento,
        RoleUsuario role,
        String fotoUrl,
        String email,
        OffsetDateTime criadoEm
) {
}