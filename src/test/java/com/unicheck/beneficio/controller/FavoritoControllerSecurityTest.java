package com.unicheck.beneficio.controller;

import com.unicheck.beneficio.service.BeneficioService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * @apiNote Autorização do FavoritoController, restrito ao ALUNO na classe inteira.
 *
 *          O favorito pertence ao aluno autenticado: o ADMIN não tem favoritos, e o RA sai sempre
 *          do token, o que impede mexer no favorito de outra pessoa.
 * @author Mateus Alves Costa
 * @since 19.09.2026
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = FavoritoControllerSecurityTest.Config.class)
class FavoritoControllerSecurityTest {
    @Autowired FavoritoController controller;
    @Autowired BeneficioService service;

    /**
     * @apiNote Limpa o histórico de chamadas do mock entre os testes.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @AfterEach
    void limpar() {
        clearInvocations(service);
    }

    /**
     * @apiNote Admin não lista favoritos, porque não tem favoritos.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Test @WithMockUser(roles = "ADMIN")
    void adminNaoListaFavoritos() {
        assertThrows(AccessDeniedException.class, () -> controller.listar(jwt("ADMIN0001"), 0, 12));
        verify(service, never()).listarFavoritos(any(), any());
    }

    /**
     * @apiNote Admin não favorita.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Test @WithMockUser(roles = "ADMIN")
    void adminNaoFavorita() {
        assertThrows(AccessDeniedException.class, () -> controller.favoritar(1L, jwt("ADMIN0001")));
        verify(service, never()).favoritar(any(), any());
    }

    /**
     * @apiNote Admin não desfavorita.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Test @WithMockUser(roles = "ADMIN")
    void adminNaoDesfavorita() {
        assertThrows(AccessDeniedException.class, () -> controller.desfavoritar(1L, jwt("ADMIN0001")));
        verify(service, never()).desfavoritar(any(), any());
    }

    /**
     * @apiNote Mesmo com um Jwt em mãos, sem Authentication no contexto a operação não passa.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Test
    void semAutenticacaoNaoFavorita() {
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> controller.favoritar(1L, jwt("RA001")));
        verify(service, never()).favoritar(any(), any());
    }

    /**
     * @apiNote Aluno percorre listar, favoritar e desfavoritar com os status esperados.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Test @WithMockUser(roles = "ALUNO")
    void alunoListaFavoritaEDesfavorita() {
        assertEquals(200, controller.listar(jwt("RA001"), 0, 12).getStatusCode().value());
        assertEquals(204, controller.favoritar(1L, jwt("RA001")).getStatusCode().value());
        assertEquals(204, controller.desfavoritar(1L, jwt("RA001")).getStatusCode().value());
    }

    /**
     * @apiNote O RA repassado à Service é o do token; a URL carrega apenas o id do benefício.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Test @WithMockUser(roles = "ALUNO")
    void oDonoDoFavoritoSaiDoTokenENaoDaUrl() {
        controller.favoritar(7L, jwt("RA001"));
        controller.desfavoritar(7L, jwt("RA001"));

        verify(service).favoritar(eq("RA001"), eq(7L));
        verify(service).desfavoritar(eq("RA001"), eq(7L));
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
            lenient().when(mock.listarFavoritos(any(), any())).thenReturn(Page.empty());
            return mock;
        }

        @Bean FavoritoController controller(BeneficioService service) {
            return new FavoritoController(service);
        }
    }
}
