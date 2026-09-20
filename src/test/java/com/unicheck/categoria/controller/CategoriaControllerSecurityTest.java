package com.unicheck.categoria.controller;

import com.unicheck.categoria.dto.CategoriaRequestDTO;
import com.unicheck.categoria.dto.CategoriaResponseDTO;
import com.unicheck.categoria.service.CategoriaService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
 * @apiNote Autorização do CategoriaController: leitura liberada a qualquer autenticado; criar,
 *          atualizar, ativar, desativar e remover são do ADMIN.
 *
 *          Ativar e desativar são os pontos novos: como mudam o que o cadastro de benefício pode
 *          oferecer, ficar de fora da restrição deixaria um aluno alterar a operação.
 * @author Mateus Alves Costa
 * @since 19.09.2026
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = CategoriaControllerSecurityTest.Config.class)
class CategoriaControllerSecurityTest {
    @Autowired CategoriaController controller;
    @Autowired CategoriaService service;

    /**
     * @apiNote Publica uma requisição no contexto, de que o cadastrar precisa para montar o Location.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @BeforeEach
    void prepararRequisicao() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    }

    /**
     * @apiNote Limpa a requisição e o histórico de chamadas entre os testes.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @AfterEach
    void limparRequisicao() {
        RequestContextHolder.resetRequestAttributes();
        clearInvocations(service);
    }

    /**
     * @apiNote Aluno não cadastra categoria.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Test @WithMockUser(roles = "ALUNO")
    void alunoNaoCadastraCategoria() {
        assertThrows(AccessDeniedException.class, () -> controller.cadastrar(new CategoriaRequestDTO("X")));
        verify(service, never()).create(any());
    }

    /**
     * @apiNote Aluno não renomeia categoria.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Test @WithMockUser(roles = "ALUNO")
    void alunoNaoAtualizaCategoria() {
        assertThrows(AccessDeniedException.class, () -> controller.atualizar(1L, new CategoriaRequestDTO("X")));
        verify(service, never()).update(any(), any());
    }

    /**
     * @apiNote Aluno não reativa categoria.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Test @WithMockUser(roles = "ALUNO")
    void alunoNaoAtivaCategoria() {
        assertThrows(AccessDeniedException.class, () -> controller.ativar(1L));
        verify(service, never()).ativar(any());
    }

    /**
     * @apiNote Aluno não desativa categoria.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Test @WithMockUser(roles = "ALUNO")
    void alunoNaoDesativaCategoria() {
        assertThrows(AccessDeniedException.class, () -> controller.desativar(1L));
        verify(service, never()).desativar(any());
    }

    /**
     * @apiNote Aluno não remove categoria.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Test @WithMockUser(roles = "ALUNO")
    void alunoNaoRemoveCategoria() {
        assertThrows(AccessDeniedException.class, () -> controller.remover(1L));
        verify(service, never()).delete(any());
    }

    /**
     * @apiNote Aluno lê a lista completa, a lista de ativas e a categoria individual.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Test @WithMockUser(roles = "ALUNO")
    void alunoListaCategoriasEAsAtivas() {
        assertEquals(200, controller.buscarTodas().getStatusCode().value());
        assertEquals(200, controller.buscarAtivas().getStatusCode().value());
        assertEquals(200, controller.buscarPorId(1L).getStatusCode().value());
    }

    /**
     * @apiNote Admin percorre criar, renomear, desativar, ativar e remover.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Test @WithMockUser(roles = "ADMIN")
    void adminExecutaTodoOCicloDeEscrita() {
        assertEquals(201, controller.cadastrar(new CategoriaRequestDTO("X")).getStatusCode().value());
        assertEquals(200, controller.atualizar(1L, new CategoriaRequestDTO("X")).getStatusCode().value());
        assertEquals(200, controller.desativar(1L).getStatusCode().value());
        assertEquals(200, controller.ativar(1L).getStatusCode().value());
        assertEquals(204, controller.remover(1L).getStatusCode().value());
    }

    /**
     * @apiNote Credencial ausente é sinalizada por AuthenticationCredentialsNotFoundException; o
     *          efeito para o cliente é o mesmo: a operação não acontece.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Test
    void semAutenticacaoNaoDesativaCategoria() {
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> controller.desativar(1L));
        verify(service, never()).desativar(any());
    }

    /**
     * @apiNote O endpoint de ativas usa o método filtrado, e não a lista completa.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Test @WithMockUser(roles = "ALUNO")
    void listarAtivasUsaOMetodoFiltradoDaService() {
        controller.buscarAtivas();

        verify(service).findAllAtivas();
        verify(service, never()).findAll();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class Config {
        @Bean CategoriaService service() {
            CategoriaService mock = mock(CategoriaService.class);
            CategoriaResponseDTO resposta = new CategoriaResponseDTO(1L, "X", true);
            lenient().when(mock.findAll()).thenReturn(List.of(resposta));
            lenient().when(mock.findAllAtivas()).thenReturn(List.of(resposta));
            lenient().when(mock.findById(any())).thenReturn(resposta);
            lenient().when(mock.create(any())).thenReturn(resposta);
            lenient().when(mock.update(any(), any())).thenReturn(resposta);
            lenient().when(mock.ativar(any())).thenReturn(resposta);
            lenient().when(mock.desativar(any())).thenReturn(new CategoriaResponseDTO(1L, "X", false));
            return mock;
        }

        @Bean CategoriaController controller(CategoriaService service) {
            return new CategoriaController(service);
        }
    }
}
