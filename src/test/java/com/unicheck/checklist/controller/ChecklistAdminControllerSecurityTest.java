package com.unicheck.checklist.controller;

import com.unicheck.checklist.service.ChecklistAdminService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ChecklistAdminControllerSecurityTest.Config.class)
class ChecklistAdminControllerSecurityTest {
    @Autowired ChecklistAdminController controller;

    @Test @WithMockUser(roles = "ALUNO")
    void alunoNaoAcessaOperacaoAdministrativa() {
        assertThrows(AccessDeniedException.class, () -> controller.listar());
    }

    @Test @WithMockUser(roles = "ADMIN")
    void adminAcessaOperacaoAdministrativa() {
        assertEquals(List.of(), controller.listar());
    }

    @Configuration(proxyBeanMethods = false) @EnableMethodSecurity
    static class Config {
        @Bean ChecklistAdminService service() { ChecklistAdminService mock = mock(ChecklistAdminService.class); when(mock.listar()).thenReturn(List.of()); return mock; }
        @Bean ChecklistAdminController controller(ChecklistAdminService service) { return new ChecklistAdminController(service); }
    }
}
