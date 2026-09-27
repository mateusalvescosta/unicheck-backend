package com.unicheck.manual.controller;

import com.unicheck.exception.GlobalExceptionHandler;
import com.unicheck.exception.ResourceConflictException;
import com.unicheck.manual.dto.AtalhoManualDTO;
import com.unicheck.manual.dto.CategoriaManualDTO;
import com.unicheck.manual.dto.ManualRequestDTO;
import com.unicheck.manual.dto.ManualResponseDTO;
import com.unicheck.manual.dto.ManualSummaryDTO;
import com.unicheck.manual.enumeration.CategoriaManual;
import com.unicheck.manual.service.ManualService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * @apiNote Contrato HTTP do ManualController: JSON de entrada e saída, status devolvidos e
 *          tradução de erro pelo GlobalExceptionHandler.
 *
 *          Usa MockMvc em modo standalone, com a Service mockada e sem segurança; a autorização
 *          fica no ManualControllerSecurityTest. O que está sob teste aqui é o que só aparece com
 *          a camada web de verdade: o Valid do corpo, a conversão do parâmetro de categoria e a
 *          desserialização dos records.
 * @author Mateus Alves Costa
 * @since 27.09.2026
 */
class ManualControllerWebTest {
    private static final String MANUAL_VALIDO = """
            {
              "categoria": "PORTAL_FERRAMENTAS_DIGITAIS",
              "tempoLeitura": 1,
              "titulo": "Portal Acadêmico",
              "resumo": "Conheça o canal.",
              "oQueSaber": "O Portal Acadêmico é o sistema.",
              "passos": [{"ordem": 1, "descricao": "Acesse o Portal."}],
              "ondeResolver": [{"nome": "Portal Acadêmico"}],
              "relacionadoIds": [4, 5]
            }
            """;

    private ManualService service;
    private MockMvc mvc;

    /**
     * @apiNote Monta o controller com a Service mockada e o handler de erros da aplicação.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @BeforeEach
    void preparar() {
        service = mock(ManualService.class);
        mvc = MockMvcBuilders.standaloneSetup(new ManualController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ---------------------------------------------------------------- leitura

    /**
     * @apiNote Busca repassa termo, categoria e página, e devolve os cards.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void buscarDevolveOsCards() throws Exception {
        when(service.buscar(any(), any(), any())).thenReturn(new PageImpl<>(List.of(
                new ManualSummaryDTO(1L, 1, CategoriaManual.MATRICULA_VIDA_ACADEMICA, "Matrícula e vida acadêmica",
                        "Rematrícula", "Renovação semestral.", 1)), PageRequest.of(1, 5), 6));

        mvc.perform(get("/manuais").param("q", "boleto").param("categoria", "MATRICULA_VIDA_ACADEMICA")
                        .param("page", "1").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].titulo").value("Rematrícula"))
                .andExpect(jsonPath("$.content[0].categoriaNome").value("Matrícula e vida acadêmica"));

        verify(service).buscar("boleto", CategoriaManual.MATRICULA_VIDA_ACADEMICA, PageRequest.of(1, 5));
    }

    /**
     * @apiNote Sem parâmetros, a busca lista tudo na primeira página de 20.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void buscarSemParametrosUsaOsPadroes() throws Exception {
        when(service.buscar(any(), any(), any())).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mvc.perform(get("/manuais")).andExpect(status().isOk());

        verify(service).buscar(null, null, PageRequest.of(0, 20));
    }

    /**
     * @apiNote Categoria fora do enum é 400 apontando o parâmetro, e não 500.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void buscarComCategoriaInexistenteDevolve400() throws Exception {
        mvc.perform(get("/manuais").param("categoria", "VIDA"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.parameter").value("categoria"));

        verifyNoInteractions(service);
    }

    /**
     * @apiNote As categorias saem com código, nomes e quantidade.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void listarCategoriasDevolveCodigoNomesEQuantidade() throws Exception {
        when(service.listarCategorias()).thenReturn(List.of(new CategoriaManualDTO(
                CategoriaManual.MATRICULA_VIDA_ACADEMICA, "Matrícula e vida acadêmica", "Vida acadêmica",
                "Matrícula, RA...", 9)));

        mvc.perform(get("/manuais/categorias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("MATRICULA_VIDA_ACADEMICA"))
                .andExpect(jsonPath("$[0].nomeCurto").value("Vida acadêmica"))
                .andExpect(jsonPath("$[0].quantidadeManuais").value(9));
    }

    /**
     * @apiNote Os atalhos saem só com o manual e o nome.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void listarAtalhosDevolveManualENome() throws Exception {
        when(service.listarAtalhos()).thenReturn(List.of(new AtalhoManualDTO(3L, "Emitir boleto")));

        mvc.perform(get("/manuais/atalhos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].manualId").value(3))
                .andExpect(jsonPath("$[0].nome").value("Emitir boleto"));
    }

    /**
     * @apiNote Manual inexistente é 404 com a mensagem da Service.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void findByIdDeManualInexistenteDevolve404() throws Exception {
        when(service.findById(99L)).thenThrow(new EntityNotFoundException("Manual não encontrado com id: 99"));

        mvc.perform(get("/manuais/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Manual não encontrado com id: 99"));
    }

    /**
     * @apiNote Id que não é número é 400, e não 500.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void findByIdComIdNaoNumericoDevolve400() throws Exception {
        mvc.perform(get("/manuais/abc")).andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    // ---------------------------------------------------------------- escrita

    /**
     * @apiNote O cadastro devolve 201 com Location, e o JSON chega inteiro na Service, inclusive
     *          as listas aninhadas.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void createDevolve201ComLocation() throws Exception {
        when(service.create(any())).thenReturn(resposta(10L));

        mvc.perform(post("/manuais").contentType(MediaType.APPLICATION_JSON).content(MANUAL_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/manuais/10"))
                .andExpect(jsonPath("$.id").value(10));

        ArgumentCaptor<ManualRequestDTO> recebido = ArgumentCaptor.forClass(ManualRequestDTO.class);
        verify(service).create(recebido.capture());
        assertEquals("O Portal Acadêmico é o sistema.", recebido.getValue().oQueSaber());
        assertEquals("Acesse o Portal.", recebido.getValue().passos().get(0).descricao());
        assertNull(recebido.getValue().ondeResolver().get(0).url());
        assertEquals(List.of(4L, 5L), recebido.getValue().relacionadoIds());
    }

    /**
     * @apiNote Corpo inválido é 400 listando os campos, e a Service nem é chamada.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void createComCorpoInvalidoDevolve400ComOsCampos() throws Exception {
        String invalido = """
                {
                  "tempoLeitura": 0,
                  "titulo": " ",
                  "resumo": "Resumo",
                  "oQueSaber": "Saber",
                  "linkTexto": "Abrir Portal",
                  "relacionadoIds": [1, 2, 3, 4]
                }
                """;

        mvc.perform(post("/manuais").contentType(MediaType.APPLICATION_JSON).content(invalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field").value(hasItems(
                        "categoria", "tempoLeitura", "titulo", "linkCompleto", "relacionadoIds")));

        verifyNoInteractions(service);
    }

    /**
     * @apiNote "Ver orientação" sem texto chega como erro de campo, não como 500.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void createComProximoPassoSemTextoDevolve400() throws Exception {
        String semTexto = MANUAL_VALIDO.replace("\"relacionadoIds\"", "\"proximoPassoManualId\": 3, \"relacionadoIds\"");

        mvc.perform(post("/manuais").contentType(MediaType.APPLICATION_JSON).content(semTexto))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("proximoPassoCompleto")));
    }

    /**
     * @apiNote JSON malformado é erro do cliente.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void createComJsonMalformadoDevolve400() throws Exception {
        mvc.perform(post("/manuais").contentType(MediaType.APPLICATION_JSON).content("{\"titulo\": "))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    /**
     * @apiNote Categoria fora do enum no corpo é erro do cliente.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void createComCategoriaInexistenteNoCorpoDevolve400() throws Exception {
        String categoriaInvalida = MANUAL_VALIDO.replace("PORTAL_FERRAMENTAS_DIGITAIS", "FERRAMENTAS");

        mvc.perform(post("/manuais").contentType(MediaType.APPLICATION_JSON).content(categoriaInvalida))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    /**
     * @apiNote Conflito lançado pela Service é 409 com a mensagem dela.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void createComTituloRepetidoDevolve409() throws Exception {
        when(service.create(any()))
                .thenThrow(new ResourceConflictException("Já existe um manual com o título 'Portal Acadêmico'."));

        mvc.perform(post("/manuais").contentType(MediaType.APPLICATION_JSON).content(MANUAL_VALIDO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Já existe um manual com o título 'Portal Acadêmico'."));
    }

    /**
     * @apiNote A atualização também passa pelo Valid: corpo incompleto não chega à Service.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void updateComCorpoInvalidoDevolve400() throws Exception {
        mvc.perform(put("/manuais/1").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    /**
     * @apiNote Atualização válida devolve 200 com o manual e usa o id da URL.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void updateDevolve200() throws Exception {
        when(service.update(eq(1L), any())).thenReturn(resposta(1L));

        mvc.perform(put("/manuais/1").contentType(MediaType.APPLICATION_JSON).content(MANUAL_VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    /**
     * @apiNote Atualizar manual inexistente é 404.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void updateDeManualInexistenteDevolve404() throws Exception {
        when(service.update(eq(99L), any())).thenThrow(new EntityNotFoundException("Manual não encontrado com id: 99"));

        mvc.perform(put("/manuais/99").contentType(MediaType.APPLICATION_JSON).content(MANUAL_VALIDO))
                .andExpect(status().isNotFound());
    }

    /**
     * @apiNote Remoção devolve 204 sem corpo.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void deleteDevolve204() throws Exception {
        mvc.perform(delete("/manuais/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(service).delete(1L);
    }

    /**
     * @apiNote Remover manual inexistente é 404.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void deleteDeManualInexistenteDevolve404() throws Exception {
        doThrow(new EntityNotFoundException("Manual não encontrado com id: 99")).when(service).delete(99L);

        mvc.perform(delete("/manuais/99")).andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------- atalhos

    /**
     * @apiNote A lista de atalhos chega à Service na ordem em que foi enviada.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void substituirAtalhosRepassaAListaNaOrdemEnviada() throws Exception {
        when(service.substituirAtalhos(anyList())).thenReturn(List.of());

        mvc.perform(put("/manuais/atalhos").contentType(MediaType.APPLICATION_JSON).content("""
                        {"atalhos": [
                          {"manualId": 7, "nome": "Acessar o Portal"},
                          {"manualId": 3, "nome": "Emitir boleto"}
                        ]}
                        """))
                .andExpect(status().isOk());

        verify(service).substituirAtalhos(List.of(
                new AtalhoManualDTO(7L, "Acessar o Portal"),
                new AtalhoManualDTO(3L, "Emitir boleto")));
    }

    /**
     * @apiNote Nove atalhos é 400, e a lista atual não é tocada.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void substituirAtalhosComMaisDeOitoDevolve400() throws Exception {
        StringBuilder nove = new StringBuilder("{\"atalhos\": [");
        for(int i = 1; i <= 9; i++) {
            if(i > 1) nove.append(',');
            nove.append("{\"manualId\": ").append(i).append(", \"nome\": \"Atalho ").append(i).append("\"}");
        }
        nove.append("]}");

        mvc.perform(put("/manuais/atalhos").contentType(MediaType.APPLICATION_JSON).content(nove.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("atalhos")));

        verifyNoInteractions(service);
    }

    /**
     * @apiNote Corpo sem a lista é 400; para remover todos os atalhos, envia-se a lista vazia.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void substituirAtalhosSemListaDevolve400() throws Exception {
        mvc.perform(put("/manuais/atalhos").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    /**
     * @apiNote Manual repetido nos atalhos é 409.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void substituirAtalhosComManualRepetidoDevolve409() throws Exception {
        when(service.substituirAtalhos(anyList()))
                .thenThrow(new ResourceConflictException("Um mesmo manual não pode aparecer em mais de um atalho."));

        mvc.perform(put("/manuais/atalhos").contentType(MediaType.APPLICATION_JSON).content("""
                        {"atalhos": [{"manualId": 7, "nome": "A"}, {"manualId": 7, "nome": "B"}]}
                        """))
                .andExpect(status().isConflict());
    }

    /**
     * @apiNote "/manuais/atalhos" não pode ser confundido com "/manuais/{id}": o PUT dos atalhos
     *          nunca chega ao update.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void rotaDeAtalhosNaoEConfundidaComId() throws Exception {
        when(service.substituirAtalhos(anyList())).thenReturn(List.of());

        mvc.perform(put("/manuais/atalhos").contentType(MediaType.APPLICATION_JSON).content("{\"atalhos\": []}"))
                .andExpect(status().isOk());

        verify(service, never()).update(any(), any());
    }

    private ManualResponseDTO resposta(Long id) {
        return new ManualResponseDTO(id, 1, CategoriaManual.PORTAL_FERRAMENTAS_DIGITAIS,
                "Portal e ferramentas digitais", 1, "Portal Acadêmico", "Conheça o canal.", null, null, null,
                "O Portal Acadêmico é o sistema.", List.of(), List.of(), List.of(), List.of(), List.of(), null,
                List.of(), List.of(), null, null, null, List.of(), null, null);
    }
}
