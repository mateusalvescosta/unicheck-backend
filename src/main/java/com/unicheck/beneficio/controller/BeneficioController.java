package com.unicheck.beneficio.controller;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.unicheck.beneficio.dto.BeneficioRequestDTO;
import com.unicheck.beneficio.dto.BeneficioResponseDTO;
import com.unicheck.beneficio.dto.BeneficioSummaryDTO;
import com.unicheck.beneficio.enumeration.TipoBeneficio;
import com.unicheck.beneficio.service.BeneficioService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * @apiNote Endpoints da entidade "Beneficio". A leitura é liberada para qualquer
 *          usuário autenticado; a escrita é restrita ao ADMIN.
 * @author Mateus Alves Costa
 * @since 18.08.2026
 * @updated 19.09.2026
 */
@Validated
@RestController
@RequestMapping("/beneficios")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class BeneficioController {

    private final BeneficioService service;

    /**
     * @apiNote Busca da listagem, com filtros opcionais de texto, tipo e categoria.
     *          Benefícios de categoria desativada continuam aparecendo.
     * @param q           --> Termo de busca livre, ou ausente para não filtrar.
     * @param tipo        --> Tipo do benefício, ou ausente para não filtrar.
     * @param categoriaId --> ID da categoria, ou ausente para não filtrar.
     * @param jwt         --> Token do usuário autenticado, usado para marcar os
     *                    favoritos do aluno.
     * @param pageable    --> Configuração de paginação; a ordenação é fixa por nome
     *                    e o parâmetro sort é ignorado.
     * @return --> Retorna 200 com uma página dos benefícios encontrados.
     * @author Mateus Alves Costa
     * @since 18.08.2026
     * @updated 19.09.2026
     */
    @GetMapping
    public ResponseEntity<Page<BeneficioSummaryDTO>> buscar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) TipoBeneficio tipo,
            @RequestParam(required = false) Long categoriaId,
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 12) Pageable pageable) {
        return ResponseEntity.ok(service.buscar(q, tipo, categoriaId, jwt.getSubject(), pageable));
    }

    /**
     * @apiNote Lista os tipos de benefício aceitos, para montar o filtro na tela.
     * @return --> Retorna 200 com todos os valores de TipoBeneficio.
     * @author Mateus Alves Costa
     * @since 18.08.2026
     */
    @GetMapping("/tipos")
    public ResponseEntity<List<TipoBeneficio>> listarTipos() {
        return ResponseEntity.ok(Arrays.asList(TipoBeneficio.values()));
    }

    /**
     * @apiNote Busca um benefício pelo seu ID, com o conteúdo do card expandido.
     * @param id --> ID do benefício a ser buscado.
     * @return --> Retorna 200 com os dados completos do benefício.
     * @author Mateus Alves Costa
     * @since 18.08.2026
     */
    @GetMapping("/{id}")
    public ResponseEntity<BeneficioResponseDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    /**
     * @apiNote Cadastra um novo benefício. Restrito ao ADMIN.
     * @param dto --> Dados para criação do benefício.
     * @return --> Retorna 201 com o benefício criado e o cabeçalho Location.
     * @author Mateus Alves Costa
     * @since 18.08.2026
     * @updated 19.09.2026
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BeneficioResponseDTO> create(@Valid @RequestBody BeneficioRequestDTO dto) {
        BeneficioResponseDTO beneficio = service.create(dto);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
            .buildAndExpand(beneficio.id()).toUri();
        return ResponseEntity.created(uri).body(beneficio);
    }

    /**
     * @apiNote Atualiza um benefício existente. Campo ausente significa "não mexe".
     *          Restrito ao ADMIN.
     * @param id  --> ID do benefício a ser atualizado.
     * @param dto --> Dados para atualização do benefício.
     * @return --> Retorna 200 com o benefício atualizado.
     * @author Mateus Alves Costa
     * @since 18.08.2026
     * @updated 19.09.2026
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BeneficioResponseDTO> update(@PathVariable Long id, @RequestBody BeneficioRequestDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    /**
     * @apiNote Remove um benefício em definitivo. Restrito ao ADMIN.
     * @param id --> ID do benefício a ser removido.
     * @return --> Retorna 204 sem corpo.
     * @author Mateus Alves Costa
     * @since 18.08.2026
     * @updated 19.09.2026
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
