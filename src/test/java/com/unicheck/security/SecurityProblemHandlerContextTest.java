package com.unicheck.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = SecurityProblemHandlerContextTest.Configuracao.class)
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
class SecurityProblemHandlerContextTest {
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SecurityProblemHandler problemHandler;

    @Autowired
    private TrocaSenhaObrigatoriaFilter trocaSenhaObrigatoriaFilter;

    @Test
    void carregaCadeiaComObjectMapperJacksonTresAutoconfigurado() {
        assertNotNull(objectMapper);
        assertNotNull(problemHandler);
        assertNotNull(trocaSenhaObrigatoriaFilter);
    }

    @Test
    void serializaProblemDetailDaRespostaNaoAutenticada() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/usuarios");
        MockHttpServletResponse response = new MockHttpServletResponse();

        problemHandler.commence(request, response, new InsufficientAuthenticationException("teste"));

        assertProblemDetail(response, HttpStatus.UNAUTHORIZED, "Falha na autenticacao",
                "Autenticacao ausente ou invalida.", "/usuarios");
    }

    @Test
    void serializaProblemDetailDaRespostaSemAutorizacao() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admins");
        MockHttpServletResponse response = new MockHttpServletResponse();

        problemHandler.handle(request, response, new AccessDeniedException("teste"));

        assertProblemDetail(response, HttpStatus.FORBIDDEN, "Acesso negado",
                "Nao foi possivel concluir esta operacao.", "/admins");
    }

    private void assertProblemDetail(MockHttpServletResponse response, HttpStatus status, String titulo,
                                     String detalhe, String instancia) throws Exception {
        assertEquals(status.value(), response.getStatus());
        assertEquals("application/problem+json", response.getContentType());
        JsonNode json = objectMapper.readTree(response.getContentAsByteArray());
        assertEquals(status.value(), json.get("status").asInt());
        assertEquals(titulo, json.get("title").asText());
        assertEquals(detalhe, json.get("detail").asText());
        assertEquals(instancia, json.get("instance").asText());
    }

    @TestConfiguration(proxyBeanMethods = false)
    @Import({SecurityProblemHandler.class, TrocaSenhaObrigatoriaFilter.class})
    static class Configuracao {
    }
}
