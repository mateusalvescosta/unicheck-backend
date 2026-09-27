package com.unicheck.manual.controller;

import com.unicheck.manual.dto.AtalhosManualRequestDTO;
import com.unicheck.manual.dto.ManualRequestDTO;
import com.unicheck.manual.dto.ManualResponseDTO;
import com.unicheck.manual.enumeration.CategoriaManual;
import com.unicheck.manual.service.ManualService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @apiNote Autorização do ManualController: leitura liberada a ALUNO e ADMIN, escrita
 *          (inclusive a escolha dos atalhos) restrita ao ADMIN, e nada para outro papel.
 *
 *          O controller é chamado direto, sem camada web, então o que está sob teste é o
 *          PreAuthorize resolvido pelo EnableMethodSecurity, e não o roteamento HTTP.
 * @author Mateus Alves Costa
 * @since 27.09.2026
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ManualControllerSecurityTest.Config.class)
class ManualControllerSecurityTest {
    @Autowired ManualController controller;
    @Autowired ManualService service;

    /**
     * @apiNote Publica uma requisição no contexto, de que o create precisa para montar o Location.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @BeforeEach
    void prepararRequisicao() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    }

    /**
     * @apiNote Limpa a requisição e o histórico de chamadas entre os testes.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @AfterEach
    void limparRequisicao() {
        RequestContextHolder.resetRequestAttributes();
        clearInvocations(service);
    }

    /**
     * @apiNote Aluno não cadastra, não atualiza, não remove e não escolhe atalhos.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test @WithMockUser(roles = "ALUNO")
    void alunoNaoEscreve() {
        assertThrows(AccessDeniedException.class, () -> controller.create(request()));
        assertThrows(AccessDeniedException.class, () -> controller.update(1L, request()));
        assertThrows(AccessDeniedException.class, () -> controller.delete(1L));
        assertThrows(AccessDeniedException.class,
                () -> controller.substituirAtalhos(new AtalhosManualRequestDTO(List.of())));
        verifyNoInteractions(service);
    }

    /**
     * @apiNote Aluno lê a busca, as categorias, os atalhos e a página do manual.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test @WithMockUser(roles = "ALUNO")
    void alunoLe() {
        assertEquals(200, controller.buscar("boleto", CategoriaManual.UTILIDADES_INSTITUCIONAIS, 0, 20)
                .getStatusCode().value());
        assertEquals(200, controller.listarCategorias().getStatusCode().value());
        assertEquals(200, controller.listarAtalhos().getStatusCode().value());
        assertEquals(200, controller.findById(1L).getStatusCode().value());
        verify(service).buscar(eq("boleto"), eq(CategoriaManual.UTILIDADES_INSTITUCIONAIS), any());
    }

    /**
     * @apiNote Admin percorre o ciclo de escrita completo, com os status esperados.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test @WithMockUser(roles = "ADMIN")
    void adminCadastraAtualizaRemoveEEscolheAtalhos() {
        assertEquals(201, controller.create(request()).getStatusCode().value());
        assertEquals(200, controller.update(1L, request()).getStatusCode().value());
        assertEquals(204, controller.delete(1L).getStatusCode().value());
        assertEquals(200, controller.substituirAtalhos(new AtalhosManualRequestDTO(List.of()))
                .getStatusCode().value());
    }

    /**
     * @apiNote O ADMIN não perde o acesso de leitura por ser quem escreve.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test @WithMockUser(roles = "ADMIN")
    void adminTambemLe() {
        assertEquals(200, controller.buscar(null, null, 0, 20).getStatusCode().value());
        assertEquals(200, controller.listarCategorias().getStatusCode().value());
        assertEquals(200, controller.listarAtalhos().getStatusCode().value());
        assertEquals(200, controller.findById(1L).getStatusCode().value());
    }

    /**
     * @apiNote Um token válido com papel que não seja ALUNO nem ADMIN não lê nem escreve: a leitura
     *          exige um dos dois papéis, e não apenas estar autenticado.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test @WithMockUser(roles = "VISITANTE")
    void papelDesconhecidoNaoLeNemEscreve() {
        assertThrows(AccessDeniedException.class, () -> controller.buscar(null, null, 0, 20));
        assertThrows(AccessDeniedException.class, () -> controller.listarCategorias());
        assertThrows(AccessDeniedException.class, () -> controller.listarAtalhos());
        assertThrows(AccessDeniedException.class, () -> controller.findById(1L));
        assertThrows(AccessDeniedException.class, () -> controller.create(request()));
        assertThrows(AccessDeniedException.class,
                () -> controller.substituirAtalhos(new AtalhosManualRequestDTO(List.of())));
        verifyNoInteractions(service);
    }

    /**
     * @apiNote Sem Authentication no contexto nem a leitura nem a escrita passam.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void semAutenticacaoNaoLeNemEscreve() {
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> controller.buscar(null, null, 0, 20));
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> controller.findById(1L));
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> controller.create(request()));
        verifyNoInteractions(service);
    }

    private ManualRequestDTO request() {
        return new ManualRequestDTO(null, CategoriaManual.PORTAL_FERRAMENTAS_DIGITAIS, 1, "Portal Acadêmico",
                "Resumo", null, null, null, "Saber", null, null, null, null, null, null, null, null, null,
                null, null, null);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class Config {
        @Bean ManualService service() {
            ManualService mock = mock(ManualService.class);
            ManualResponseDTO resposta = new ManualResponseDTO(1L, 1, CategoriaManual.PORTAL_FERRAMENTAS_DIGITAIS,
                    "Portal e ferramentas digitais", 1, "Portal Acadêmico", "Resumo", null, null, null, "Saber",
                    List.of(), List.of(), List.of(), List.of(), List.of(), null, List.of(), List.of(), null, null,
                    null, List.of(), null, null);
            lenient().when(mock.buscar(any(), any(), any())).thenReturn(Page.empty());
            lenient().when(mock.findById(any())).thenReturn(resposta);
            lenient().when(mock.create(any())).thenReturn(resposta);
            lenient().when(mock.update(any(), any())).thenReturn(resposta);
            return mock;
        }

        @Bean ManualController controller(ManualService service) {
            return new ManualController(service);
        }
    }
}
