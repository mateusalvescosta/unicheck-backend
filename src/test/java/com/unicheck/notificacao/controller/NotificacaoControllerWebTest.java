package com.unicheck.notificacao.controller;

import com.unicheck.exception.GlobalExceptionHandler;
import com.unicheck.exception.ResourceNotFoundException;
import com.unicheck.notificacao.dto.NotificacaoResponseDTO;
import com.unicheck.notificacao.dto.PreferenciaNotificacaoDTO;
import com.unicheck.notificacao.dto.TotalNaoLidasResponseDTO;
import com.unicheck.notificacao.enumeration.TipoNotificacao;
import com.unicheck.notificacao.service.NotificacaoAlunoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * @apiNote Contrato HTTP do NotificacaoController com a Service mockada e sem segurança (a
 *          autorização fica no NotificacaoControllerSecurityTest). Um resolver de teste entrega
 *          um JWT fixo no lugar do @AuthenticationPrincipal.
 */
class NotificacaoControllerWebTest {
    private NotificacaoAlunoService service;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        service = mock(NotificacaoAlunoService.class);
        mvc = MockMvcBuilders.standaloneSetup(new NotificacaoController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new JwtFixo("RA001"))
                .build();
    }

    @Test
    void listarDevolveAPaginaComOObjetoDoTipo() throws Exception {
        when(service.listar(eq("RA001"), eq(null), any())).thenReturn(new PageImpl<>(List.of(
                new NotificacaoResponseDTO(15L, TipoNotificacao.MANUAL_NOVO, "Novo manual: Portal", false,
                        OffsetDateTime.now(), null, null, null)), PageRequest.of(0, 20), 1));

        mvc.perform(get("/notificacoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].tipo").value("MANUAL_NOVO"))
                .andExpect(jsonPath("$.content[0].mensagem").value("Novo manual: Portal"))
                .andExpect(jsonPath("$.content[0].lida").value(false));

        verify(service).listar("RA001", null, PageRequest.of(0, 20));
    }

    @Test
    void listarRepassaFiltroEPaginacao() throws Exception {
        when(service.listar(anyString(), any(), any())).thenReturn(new PageImpl<>(List.of(), PageRequest.of(2, 5), 0));

        mvc.perform(get("/notificacoes").param("lida", "false").param("page", "2").param("size", "5"))
                .andExpect(status().isOk());

        verify(service).listar("RA001", false, PageRequest.of(2, 5));
    }

    @Test
    void totalNaoLidasDevolveOContador() throws Exception {
        when(service.totalNaoLidas("RA001")).thenReturn(new TotalNaoLidasResponseDTO(3));

        mvc.perform(get("/notificacoes/nao-lidas/total"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3));
    }

    @Test
    void marcarComoLidaResponde204() throws Exception {
        mvc.perform(patch("/notificacoes/7/lida")).andExpect(status().isNoContent());

        verify(service).marcarComoLida(7L, "RA001");
    }

    @Test
    void marcarComoLidaDeOutroAlunoResponde404() throws Exception {
        doThrow(new ResourceNotFoundException("Notificacao nao encontrada")).when(service).marcarComoLida(8L, "RA001");

        mvc.perform(patch("/notificacoes/8/lida")).andExpect(status().isNotFound());
    }

    @Test
    void marcarTodasComoLidasResponde204() throws Exception {
        mvc.perform(patch("/notificacoes/lidas")).andExpect(status().isNoContent());

        verify(service).marcarTodasComoLidas("RA001");
    }

    @Test
    void listarPreferenciasDevolveOsTipos() throws Exception {
        when(service.listarPreferencias("RA001")).thenReturn(List.of(
                new PreferenciaNotificacaoDTO(TipoNotificacao.BENEFICIO_NOVO, true),
                new PreferenciaNotificacaoDTO(TipoNotificacao.MANUAL_NOVO, false)));

        mvc.perform(get("/notificacoes/preferencias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1].tipo").value("MANUAL_NOVO"))
                .andExpect(jsonPath("$[1].ativo").value(false));
    }

    @Test
    void alterarPreferenciaResponde204() throws Exception {
        mvc.perform(put("/notificacoes/preferencias/MANUAL_NOVO").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ativo\":false}"))
                .andExpect(status().isNoContent());

        verify(service).alterarPreferencia("RA001", TipoNotificacao.MANUAL_NOVO, false);
    }

    @Test
    void alterarPreferenciaDeTipoInexistenteResponde400() throws Exception {
        mvc.perform(put("/notificacoes/preferencias/QUALQUER").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ativo\":false}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void alterarPreferenciaSemAtivoResponde400() throws Exception {
        mvc.perform(put("/notificacoes/preferencias/MANUAL_NOVO").contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("ativo"));

        verifyNoInteractions(service);
    }

    /** Entrega um JWT com o RA informado para parâmetros anotados com @AuthenticationPrincipal. */
    private record JwtFixo(String ra) implements HandlerMethodArgumentResolver {
        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
        }

        @Override
        public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                      NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
            return Jwt.withTokenValue("token").header("alg", "HS256").subject(ra)
                    .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(3600)).build();
        }
    }
}
