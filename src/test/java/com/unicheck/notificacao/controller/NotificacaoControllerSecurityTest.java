package com.unicheck.notificacao.controller;

import com.unicheck.notificacao.dto.AlterarPreferenciaRequestDTO;
import com.unicheck.notificacao.enumeration.TipoNotificacao;
import com.unicheck.notificacao.service.NotificacaoAlunoService;
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
import static org.mockito.Mockito.*;

/**
 * @apiNote Autorização do NotificacaoController, restrito ao ALUNO na classe inteira. As
 *          notificações pertencem ao aluno autenticado e o RA sai sempre do token.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = NotificacaoControllerSecurityTest.Config.class)
class NotificacaoControllerSecurityTest {
    @Autowired NotificacaoController controller;
    @Autowired NotificacaoAlunoService service;

    @AfterEach
    void limpar() {
        clearInvocations(service);
    }

    @Test @WithMockUser(roles = "ADMIN")
    void adminNaoAcessaNenhumaRota() {
        Jwt jwt = jwt("ADMIN0001");
        assertThrows(AccessDeniedException.class, () -> controller.listar(jwt, null, 0, 20));
        assertThrows(AccessDeniedException.class, () -> controller.totalNaoLidas(jwt));
        assertThrows(AccessDeniedException.class, () -> controller.marcarComoLida(1L, jwt));
        assertThrows(AccessDeniedException.class, () -> controller.marcarTodasComoLidas(jwt));
        assertThrows(AccessDeniedException.class, () -> controller.listarPreferencias(jwt));
        assertThrows(AccessDeniedException.class, () -> controller.alterarPreferencia(
                TipoNotificacao.MANUAL_NOVO, new AlterarPreferenciaRequestDTO(false), jwt));
        verifyNoInteractions(service);
    }

    @Test
    void semAutenticacaoNaoLista() {
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> controller.listar(jwt("RA001"), null, 0, 20));
        verifyNoInteractions(service);
    }

    @Test @WithMockUser(roles = "ALUNO")
    void alunoUsaTodasAsRotas() {
        Jwt jwt = jwt("RA001");
        assertEquals(200, controller.listar(jwt, null, 0, 20).getStatusCode().value());
        assertEquals(200, controller.totalNaoLidas(jwt).getStatusCode().value());
        assertEquals(204, controller.marcarComoLida(1L, jwt).getStatusCode().value());
        assertEquals(204, controller.marcarTodasComoLidas(jwt).getStatusCode().value());
        assertEquals(200, controller.listarPreferencias(jwt).getStatusCode().value());
        assertEquals(204, controller.alterarPreferencia(TipoNotificacao.MANUAL_NOVO,
                new AlterarPreferenciaRequestDTO(false), jwt).getStatusCode().value());
    }

    @Test @WithMockUser(roles = "ALUNO")
    void oDonoSaiDoTokenENaoDaRequisicao() {
        Jwt jwt = jwt("RA001");
        controller.marcarComoLida(7L, jwt);
        controller.alterarPreferencia(TipoNotificacao.MANUAL_NOVO, new AlterarPreferenciaRequestDTO(false), jwt);

        verify(service).marcarComoLida(7L, "RA001");
        verify(service).alterarPreferencia("RA001", TipoNotificacao.MANUAL_NOVO, false);
    }

    private Jwt jwt(String subject) {
        return Jwt.withTokenValue("token").header("alg", "HS256").subject(subject)
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(3600)).build();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class Config {
        @Bean NotificacaoAlunoService service() {
            NotificacaoAlunoService mock = mock(NotificacaoAlunoService.class);
            lenient().when(mock.listar(any(), any(), any())).thenReturn(Page.empty());
            return mock;
        }

        @Bean NotificacaoController controller(NotificacaoAlunoService service) {
            return new NotificacaoController(service);
        }
    }
}
