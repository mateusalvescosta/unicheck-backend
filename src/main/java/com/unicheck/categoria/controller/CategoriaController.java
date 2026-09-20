package com.unicheck.categoria.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.unicheck.categoria.dto.CategoriaRequestDTO;
import com.unicheck.categoria.dto.CategoriaResponseDTO;
import com.unicheck.categoria.service.CategoriaService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * @apiNote Endpoints da entidade "Categoria". A leitura é liberada para qualquer
 *          usuário autenticado; criar, atualizar, ativar, desativar e remover são
 *          restritos ao ADMIN.
 * @author Mateus Alves Costa
 * @since 18.08.2026
 * @updated 19.09.2026
 */
@Validated
@RestController
@RequestMapping("/categorias")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class CategoriaController {

    private final CategoriaService service;

    /**
     * @apiNote Lista todas as categorias, ativas e desativadas, para a tela de
     *          administração.
     * @return --> Retorna 200 com a lista completa ordenada por nome.
     * @author Mateus Alves Costa
     * @since 18.08.2026
     */
    @GetMapping
    public ResponseEntity<List<CategoriaResponseDTO>> buscarTodas() {
        return ResponseEntity.ok(service.findAll());
    }

    /**
     * @apiNote Lista somente as categorias ativas. É esta a lista que o cadastro de
     *          benefício deve oferecer, já que categoria desativada não pode ser
     *          vinculada.
     * @return --> Retorna 200 com as categorias ativas ordenadas por nome.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @GetMapping("/ativas")
    public ResponseEntity<List<CategoriaResponseDTO>> buscarAtivas() {
        return ResponseEntity.ok(service.findAllAtivas());
    }

    /**
     * @apiNote Busca uma categoria pelo seu ID, ativa ou não.
     * @param id --> ID da categoria a ser buscada.
     * @return --> Retorna 200 com os dados da categoria.
     * @author Mateus Alves Costa
     * @since 18.08.2026
     */
    @GetMapping("/{id}")
    public ResponseEntity<CategoriaResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    /**
     * @apiNote Cadastra uma nova categoria, já ativa. Restrito ao ADMIN.
     * @param dto --> Dados para criação da categoria.
     * @return --> Retorna 201 com a categoria criada e o cabeçalho Location.
     * @author Mateus Alves Costa
     * @since 18.08.2026
     * @updated 19.09.2026
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoriaResponseDTO> cadastrar(@Valid @RequestBody CategoriaRequestDTO dto) {
        CategoriaResponseDTO categoria = service.create(dto);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
            .buildAndExpand(categoria.id()).toUri();
        return ResponseEntity.created(uri).body(categoria);
    }

    /**
     * @apiNote Atualiza o nome de uma categoria. O estado ativo não muda por aqui.
     *          Restrito ao ADMIN.
     * @param id  --> ID da categoria a ser atualizada.
     * @param dto --> Dados para atualização da categoria.
     * @return --> Retorna 200 com a categoria atualizada.
     * @author Mateus Alves Costa
     * @since 18.08.2026
     * @updated 19.09.2026
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoriaResponseDTO> atualizar(@PathVariable Long id, @Valid @RequestBody CategoriaRequestDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    /**
     * @apiNote Reativa uma categoria, devolvendo-a à lista de escolha. Restrito ao
     *          ADMIN.
     * @param id --> ID da categoria a ser ativada.
     * @return --> Retorna 200 com a categoria já ativa.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @PatchMapping("/{id}/ativar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoriaResponseDTO> ativar(@PathVariable Long id) {
        return ResponseEntity.ok(service.ativar(id));
    }

    /**
     * @apiNote Desativa uma categoria. Ela sai da lista de escolha, mas os
     *          benefícios que já a usam continuam aparecendo normalmente. Restrito
     *          ao ADMIN.
     * @param id --> ID da categoria a ser desativada.
     * @return --> Retorna 200 com a categoria já desativada.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @PatchMapping("/{id}/desativar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoriaResponseDTO> desativar(@PathVariable Long id) {
        return ResponseEntity.ok(service.desativar(id));
    }

    /**
     * @apiNote Remove a categoria em definitivo. No fluxo normal prefira desativar.
     *          Restrito ao ADMIN.
     * @param id --> ID da categoria a ser removida.
     * @return --> Retorna 204 sem corpo.
     * @author Mateus Alves Costa
     * @since 18.08.2026
     * @updated 19.09.2026
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
