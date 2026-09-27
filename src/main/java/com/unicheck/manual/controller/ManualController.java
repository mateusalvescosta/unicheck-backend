package com.unicheck.manual.controller;

import java.net.URI;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.unicheck.manual.dto.AtalhoManualDTO;
import com.unicheck.manual.dto.AtalhosManualRequestDTO;
import com.unicheck.manual.dto.CategoriaManualDTO;
import com.unicheck.manual.dto.ManualRequestDTO;
import com.unicheck.manual.dto.ManualResponseDTO;
import com.unicheck.manual.dto.ManualSummaryDTO;
import com.unicheck.manual.enumeration.CategoriaManual;
import com.unicheck.manual.service.ManualService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * @apiNote Endpoints do Manual do Aluno. A leitura é liberada para qualquer
 *          usuário autenticado; a escrita é restrita ao ADMIN.
 * @author Mateus Alves Costa
 * @since 27.09.2026
 */
@Validated
@RestController
@RequestMapping("/manuais")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ManualController {

    private final ManualService service;

    /**
     * @apiNote Busca de "Qual é a sua dúvida?" e listagem por assunto. O termo é
     *          procurado em todas as partes textuais do manual.
     * @param q         --> Termo de busca livre, ou ausente para não filtrar.
     * @param categoria --> Categoria, ou ausente para listar todas ("Todos").
     * @param page      --> Página desejada, começando em zero.
     * @param size      --> Quantidade de itens por página.
     * @return --> Retorna 200 com uma página dos manuais encontrados.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @GetMapping
    public ResponseEntity<Page<ManualSummaryDTO>> buscar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) CategoriaManual categoria,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.buscar(q, categoria, PageRequest.of(page, size)));
    }

    /**
     * @apiNote Categorias de "Encontre por assunto" e dos chips de filtro, com a
     *          quantidade de manuais de cada uma.
     * @return --> Retorna 200 com todas as categorias, na ordem da tela.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @GetMapping("/categorias")
    public ResponseEntity<List<CategoriaManualDTO>> listarCategorias() {
        return ResponseEntity.ok(service.listarCategorias());
    }

    /**
     * @apiNote Lista "Atalhos do dia a dia".
     * @return --> Retorna 200 com os atalhos, na ordem da grade.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @GetMapping("/atalhos")
    public ResponseEntity<List<AtalhoManualDTO>> listarAtalhos() {
        return ResponseEntity.ok(service.listarAtalhos());
    }

    /**
     * @apiNote Escolhe quais manuais aparecem em "Atalhos do dia a dia",
     *          substituindo a lista inteira. Restrito ao ADMIN.
     * @param dto --> Atalhos na ordem de exibição.
     * @return --> Retorna 200 com os atalhos gravados.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @PutMapping("/atalhos")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AtalhoManualDTO>> substituirAtalhos(
            @Valid @RequestBody AtalhosManualRequestDTO dto) {
        return ResponseEntity.ok(service.substituirAtalhos(dto.atalhos()));
    }

    /**
     * @apiNote Busca a página completa de um manual.
     * @param id --> ID do manual.
     * @return --> Retorna 200 com os dados completos do manual.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @GetMapping("/{id}")
    public ResponseEntity<ManualResponseDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    /**
     * @apiNote Cadastra um novo manual. Restrito ao ADMIN.
     * @param dto --> Dados para criação do manual.
     * @return --> Retorna 201 com o manual criado e o cabeçalho Location.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ManualResponseDTO> create(@Valid @RequestBody ManualRequestDTO dto) {
        ManualResponseDTO manual = service.create(dto);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
            .buildAndExpand(manual.id()).toUri();
        return ResponseEntity.created(uri).body(manual);
    }

    /**
     * @apiNote Atualiza um manual por completo. Restrito ao ADMIN.
     * @param id  --> ID do manual a ser atualizado.
     * @param dto --> Dados completos do manual.
     * @return --> Retorna 200 com o manual atualizado.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ManualResponseDTO> update(@PathVariable Long id, @Valid @RequestBody ManualRequestDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    /**
     * @apiNote Remove um manual em definitivo. Restrito ao ADMIN.
     * @param id --> ID do manual a ser removido.
     * @return --> Retorna 204 sem corpo.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
