package com.unicheck.auth.controller;

import com.unicheck.auth.dto.RedefinirSenhaRequestDTO;
import com.unicheck.auth.service.AuthService;
import com.unicheck.auth.service.RecuperacaoSenhaService;
import com.unicheck.exception.GlobalExceptionHandler;
import com.unicheck.exception.LinkRedefinicaoInvalidoException;
import com.unicheck.usuario.service.AlunoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * @apiNote Contrato HTTP das rotas de recuperação de senha, com a Service mockada e sem segurança
 *          (que as rotas sejam públicas é verificado em UnicheckApplicationTests).
 */
class AuthControllerRecuperacaoSenhaWebTest {
    private static final String MENSAGEM = "Se o e-mail estiver cadastrado, enviaremos as instruções para redefinir a senha.";

    private RecuperacaoSenhaService service;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        service = mock(RecuperacaoSenhaService.class);
        mvc = MockMvcBuilders.standaloneSetup(
                        new AuthController(mock(AuthService.class), mock(AlunoService.class), service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void esqueciSenhaResponde202ComMensagemFixa() throws Exception {
        mvc.perform(post("/auth/esqueci-senha").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"aluno@example.invalid\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.mensagem").value(MENSAGEM));

        verify(service).solicitar("aluno@example.invalid");
    }

    @Test
    void esqueciSenhaDevolveOMesmoCorpoParaQualquerEmailValido() throws Exception {
        // A Service nao devolve nada nem lanca nada quando o e-mail nao existe, entao o corpo e o mesmo.
        String existente = mvc.perform(post("/auth/esqueci-senha").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"existe@example.invalid\"}"))
                .andReturn().getResponse().getContentAsString();
        String inexistente = mvc.perform(post("/auth/esqueci-senha").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nao-existe@example.invalid\"}"))
                .andReturn().getResponse().getContentAsString();

        assertEquals(existente, inexistente);
    }

    @Test
    void esqueciSenhaComEmailInvalidoOuVazioRetorna400SemChamarAService() throws Exception {
        mvc.perform(post("/auth/esqueci-senha").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"isso-nao-e-email\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/auth/esqueci-senha").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void redefinirSenhaValidaResponde204() throws Exception {
        mvc.perform(post("/auth/redefinir-senha").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"abc\",\"novaSenha\":\"novaSenha123\"}"))
                .andExpect(status().isNoContent());

        verify(service).redefinir(new RedefinirSenhaRequestDTO("abc", "novaSenha123"));
    }

    @Test
    void redefinirSenhaComLinkInvalidoRetorna400ComMensagemUnica() throws Exception {
        doThrow(new LinkRedefinicaoInvalidoException()).when(service).redefinir(any());

        mvc.perform(post("/auth/redefinir-senha").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"abc\",\"novaSenha\":\"novaSenha123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Link inválido ou expirado."));
    }

    @Test
    void redefinirSenhaCurtaOuSemTokenRetorna400SemChamarAService() throws Exception {
        mvc.perform(post("/auth/redefinir-senha").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"abc\",\"novaSenha\":\"curta\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("novaSenha"));
        mvc.perform(post("/auth/redefinir-senha").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"\",\"novaSenha\":\"novaSenha123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("token"));

        verifyNoInteractions(service);
    }
}
