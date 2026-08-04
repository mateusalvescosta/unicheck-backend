package com.unicheck.usuario.dto;

import com.unicheck.usuario.enumeration.RoleUsuario;

/**
 * DTO de SAÍDA do login.
 *
 * Devolve o token JWT que o front-end vai guardar (localStorage, cookie, etc)
 * e reenviar em toda requisição futura no header:
 *   Authorization: Bearer <token>
 *
 * Campos:
 * - token: o JWT em si.
 * - tipo: fica fixo como "Bearer" -- é só uma convenção de mercado pra deixar
 *   explícito no JSON como o token deve ser usado.
 * - role: ALUNO ou ADMIN (vem do Usuario associado). O front usa isso pra
 *   decidir pra qual tela redirecionar depois do login (dashboard de aluno
 *   ou painel de admin), sem precisar decodificar o token só pra saber isso.
 * - cargo: SÓ FAZ SENTIDO quando role = ADMIN (ex: "COORDENADOR", "SECRETARIA").
 *   Vem null quando quem logou é Aluno, já que Aluno não tem esse conceito.
 *   Serve pra, no futuro, decidir o nível de permissão dentro do próprio
 *   painel de admin -- ex: só "COORDENADOR" pode aprovar matrícula, mas
 *   "SECRETARIA" só consulta. Hoje o cargo é só uma String livre na entidade
 *   Admin, então essa regra de "quem pode o quê" ainda precisa ser decidida
 *   por vocês (pode virar um enum próprio no futuro, se crescer).
 *
 * Assim como o LoginRequestDTO, esse também NÃO passa pelo Mapper --
 * ele não vem direto de uma entidade, é montado na mão dentro da AuthService,
 * depois que o token já foi gerado:
 *
 *   // se for admin:
 *   return new LoginResponseDTO(token, "Bearer", RoleUsuario.ADMIN, admin.getCargo());
 *   // se for aluno:
 *   return new LoginResponseDTO(token, "Bearer", RoleUsuario.ALUNO, null);
 */
public record LoginResponseDTO(
        String token,
        String tipo,
        RoleUsuario role,
        String cargo,
        Boolean trocarSenhaObrigatorio // front redireciona pra tela de troca se vier true
) {
}