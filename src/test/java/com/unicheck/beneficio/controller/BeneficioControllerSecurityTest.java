package com.unicheck.beneficio.controller;

import com.unicheck.beneficio.dto.BeneficioRequestDTO;
import com.unicheck.beneficio.dto.BeneficioResponseDTO;
import com.unicheck.beneficio.enumeration.TipoBeneficio;
import com.unicheck.beneficio.service.BeneficioService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @apiNote Autorização do BeneficioController: leitura liberada a qualquer autenticado,
 *          escrita restrita ao ADMIN.
 *
 *          O controller é chamado direto, sem camada web, então o que está sob teste é o
 *          PreAuthorize resolvido pelo EnableMethodSecurity, e não o roteamento HTTP.
 * @author Mateus Alves Costa
 * @since 19.09.2026
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = BeneficioControllerSecurityTest.Config.class)
class BeneficioControllerSecurityTest {
    @Autowired BeneficioController controller;
    @Autowired BeneficioService service;

    /**
     * @apiNote Publica uma requisição no contexto, de que o create precisa para montar o Location.
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
     * @apiNote Aluno não cadastra benefício, e a regra barra antes de a Service ser chamada.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Test @WithMockUser(roles = "ALUNO")
    void alunoNaoCadastraBeneficio() {
        assertThrows(AccessDeniedException.class, () -> controller.create(request()));
        verify(service, never()).create(any());
    }

    /**
     * @apiNote Aluno não atualiza benefício.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Test @WithMockUser(roles = "ALUNO")
    void alunoNaoAtualizaBeneficio() {
        assertThrows(AccessDeniedException.class, () -> controller.update(1L, request()));
        verify(service, never()).update(any(), any());
    }

    /**
     * @apiNote Aluno não remove benefício.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Test @WithMockUser(roles = "ALUNO")
    void alunoNaoRemoveBeneficio() {
        assertThrows(AccessDeniedException.class, () -> controller.delete(1L));
        verify(service, never()).delete(any());
    }

    /**
     * @apiNote Aluno lê a listagem, o card expandido e os tipos.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Test @WithMockUser(roles = "ALUNO")
    void alunoListaEAbreBeneficio() {
        assertEquals(200, controller.buscar(null, null, null, jwt("RA001"), PageRequest.of(0, 12))
                .getStatusCode().value());
        assertEquals(200, controller.findById(1L).getStatusCode().value());
        assertEquals(200, controller.listarTipos().getStatusCode().value());
    }

    /**
     * @apiNote Admin percorre o ciclo de escrita completo, com os status esperados.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Test @WithMockUser(roles = "ADMIN")
    void adminCadastraAtualizaERemove() {
        assertEquals(201, controller.create(request()).getStatusCode().value());
        assertEquals(200, controller.update(1L, request()).getStatusCode().value());
        assertEquals(204, controller.delete(1L).getStatusCode().value());
    }

    /**
     * @apiNote O ADMIN não perde o acesso de leitura por ser quem escreve.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Test @WithMockUser(roles = "ADMIN")
    void adminTambemLe() {
        assertEquals(200, controller.buscar(null, null, null, jwt("ADMIN0001"), PageRequest.of(0, 12))
                .getStatusCode().value());
    }

    /**
     * @apiNote Sem Authentication no contexto o Spring sinaliza credencial ausente, e não acesso
     *          negado, mas a escrita fica barrada do mesmo jeito.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Test
    void semAutenticacaoNaoEscreve() {
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> controller.create(request()));
        verify(service, never()).create(any());
    }

    /**
     * @apiNote Os filtros chegam à Service como recebidos, e o RA vem do token, nunca do corpo ou da URL.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Test @WithMockUser(roles = "ALUNO")
    void buscarRepassaOsFiltrosRecebidosParaAService() {
        controller.buscar("bolsa", TipoBeneficio.GRATUITO, 3L, jwt("RA001"), PageRequest.of(1, 5));

        verify(service).buscar(eq("bolsa"), eq(TipoBeneficio.GRATUITO), eq(3L), eq("RA001"), any());
    }

    /**
     * @apiNote O endpoint de tipos expõe todas as constantes do enum.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Test @WithMockUser(roles = "ALUNO")
    void listarTiposDevolveTodasAsConstantesDoEnum() {
        assertEquals(List.of(TipoBeneficio.values()), controller.listarTipos().getBody());
    }

    /**
     * @apiNote Monta um BeneficioRequestDTO mínimo para as chamadas de escrita.
     * @return --> Retorna o corpo montado.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    private BeneficioRequestDTO request() {
        return new BeneficioRequestDTO(null, "Bolsa", "c", "d", "u", TipoBeneficio.GRATUITO, "p",
                null, null, null, null, null, null);
    }

    /**
     * @apiNote Monta um Jwt de teste com o subject informado.
     * @param subject --> RA que deve aparecer como dono da requisição.
     * @return --> Retorna o token montado.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    private Jwt jwt(String subject) {
        return Jwt.withTokenValue("token").header("alg", "HS256").subject(subject)
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(3600)).build();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class Config {
        @Bean BeneficioService service() {
            BeneficioService mock = mock(BeneficioService.class);
            BeneficioResponseDTO resposta = new BeneficioResponseDTO(1L, null, "Bolsa", "c", "d", "u",
                    TipoBeneficio.GRATUITO, "p", null, null, null, Set.of(), List.of(), Set.of());
            lenient().when(mock.buscar(any(), any(), any(), any(), any())).thenReturn(Page.empty());
            lenient().when(mock.findById(any())).thenReturn(resposta);
            lenient().when(mock.create(any())).thenReturn(resposta);
            lenient().when(mock.update(any(), any())).thenReturn(resposta);
            return mock;
        }

        @Bean BeneficioController controller(BeneficioService service) {
            return new BeneficioController(service);
        }
    }
}
