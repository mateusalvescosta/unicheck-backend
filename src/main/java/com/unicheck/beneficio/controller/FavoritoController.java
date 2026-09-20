package com.unicheck.beneficio.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.unicheck.beneficio.dto.BeneficioSummaryDTO;
import com.unicheck.beneficio.service.BeneficioService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;

/**
 * @apiNote Endpoints de "Meus favoritos". Toda a classe é restrita ao ALUNO: o
 *          favorito pertence ao aluno autenticado, e o ADMIN não tem favoritos.
 *          O dono é sempre lido do token, nunca do corpo ou da URL.
 * @author Mateus Alves Costa
 * @since 18.08.2026
 * @updated 19.09.2026
 */
@Validated
@RestController
@RequestMapping("/beneficios/favoritos")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ALUNO')")
@SecurityRequirement(name = "bearerAuth")
public class FavoritoController {

    private final BeneficioService service;

    /**
     * @apiNote Lista os benefícios favoritados pelo aluno autenticado, do mais
     *          recente para o mais antigo.
     * @param jwt      --> Token do aluno autenticado, de onde sai o RA.
     * @param pageable --> Configuração de paginação; a ordenação é fixa na consulta
     *                 e o parâmetro sort é ignorado.
     * @return --> Retorna 200 com uma página dos benefícios favoritados.
     * @author Mateus Alves Costa
     * @since 18.08.2026
     */
    @GetMapping
    public ResponseEntity<Page<BeneficioSummaryDTO>> listar(@AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 12) Pageable pageable) {
        return ResponseEntity.ok(service.listarFavoritos(jwt.getSubject(), pageable));
    }

    /**
     * @apiNote Marca um benefício como favorito do aluno autenticado. É
     *          idempotente: repetir a chamada devolve 204 do mesmo jeito.
     * @param beneficioId --> ID do benefício a ser favoritado.
     * @param jwt         --> Token do aluno autenticado, de onde sai o RA.
     * @return --> Retorna 204 sem corpo.
     * @author Mateus Alves Costa
     * @since 18.08.2026
     */
    @PutMapping("/{beneficioId}")
    public ResponseEntity<Void> favoritar(@PathVariable Long beneficioId, @AuthenticationPrincipal Jwt jwt) {
        service.favoritar(jwt.getSubject(), beneficioId);
        return ResponseEntity.noContent().build();
    }

    /**
     * @apiNote Remove a marcação de favorito do aluno autenticado.
     * @param beneficioId --> ID do benefício a ser desfavoritado.
     * @param jwt         --> Token do aluno autenticado, de onde sai o RA.
     * @return --> Retorna 204 sem corpo.
     * @author Mateus Alves Costa
     * @since 18.08.2026
     */
    @DeleteMapping("/{beneficioId}")
    public ResponseEntity<Void> desfavoritar(@PathVariable Long beneficioId, @AuthenticationPrincipal Jwt jwt) {
        service.desfavoritar(jwt.getSubject(), beneficioId);
        return ResponseEntity.noContent().build();
    }
}
