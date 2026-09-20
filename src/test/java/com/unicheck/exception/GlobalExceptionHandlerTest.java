package com.unicheck.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {
    @Test
    void senhaAtualIncorretaRetorna403SemExporDetalhes() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/usuarios/trocar-senha");

        ResponseEntity<ProblemDetail> response = new GlobalExceptionHandler()
                .accessDenied(new AccessDeniedException("Senha atual incorreta"), request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Nao foi possivel concluir esta operacao.", response.getBody().getDetail());
    }
}
