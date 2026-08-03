package com.unicheck.usuario.dto;

import com.unicheck.usuario.enumeration.RoleUsuario;

/**
 * DTO de ENTRADA (Request) do Usuario.
 *
 * É o que o cliente (front-end) manda no corpo (body) da requisição
 * pra criar um Usuario.
 *
 * Por que não usar a entidade Usuario direto na Controller?
 * 1) Segurança: a entidade tem campos como "id" e "criadoEm" que quem
 *    consome a API NÃO deveria poder definir manualmente.
 * 2) Desacoplamento: se a entidade mudar (novo campo no banco, por exemplo),
 *    o "contrato" da API não muda junto sem querer.
 *
 * Usamos "record" (recurso do Java moderno) em vez de classe com Lombok
 * porque um DTO de request é só um "carregador de dados" imutável -- não
 * precisa de setters. O record já gera construtor, getters (nome do campo,
 * sem "get"), equals, hashCode e toString sozinho.
 */
public record UsuarioRequestDTO(
        String nome,
        String senha,   // vem em texto puro aqui; quem criptografa (BCrypt) é a Service, não o DTO
        RoleUsuario role,
        String fotoUrl,
        String email
) {
}