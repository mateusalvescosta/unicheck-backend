package com.unicheck.usuario.dto;

/**
 * DTO de ENTRADA do login (endpoint tipo POST /auth/login).
 *
 * ATENÇÃO: o campo se chama "login" (genérico) e NÃO "email", de propósito --
 * porque no UniCheck o Aluno loga com RA e o Admin loga com email. Em vez de
 * criar dois endpoints diferentes (/auth/login/aluno e /auth/login/admin),
 * a gente aceita os dois no mesmo campo e quem decide o que fazer com esse
 * valor é a AuthService, tentando os dois caminhos:
 *
 *   1) aluno = alunoRepository.findByRa(dto.login())
 *      -> se achar, pega o Usuario dele: aluno.getUsuario()
 *   2) se não achou por RA, tenta usuarioRepository.findByEmail(dto.login())
 *      -> cobre o caso do Admin (ou qualquer usuário logando por email)
 *   3) se não achou em nenhum dos dois -> credenciais inválidas
 *
 * Repare que os métodos findByRa() e findByEmail() JÁ EXISTEM nos
 * repositories que fizemos -- não precisou mudar nada lá, só decidir a
 * ORDEM de tentativa na Service.
 *
 * Esse DTO não vira entidade e não passa por Mapper (não tem tradução
 * campo-a-campo automática que faça sentido aqui).
 */
public record LoginRequestDTO(
        String login,
        String senha
) {
}