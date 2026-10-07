package com.unicheck.auth.service;

import com.unicheck.auth.domain.TokenRedefinicaoSenha;
import com.unicheck.auth.dto.RecuperacaoSenhaSolicitadaEvent;
import com.unicheck.auth.dto.RedefinirSenhaRequestDTO;
import com.unicheck.auth.dto.SenhaRedefinidaEvent;
import com.unicheck.auth.repository.TokenRedefinicaoSenhaRepository;
import com.unicheck.exception.LinkRedefinicaoInvalidoException;
import com.unicheck.usuario.domain.Usuario;
import com.unicheck.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * @apiNote Regras do fluxo de recuperação de senha com os repositories mockados: o que é gravado,
 *          quando nada é enviado e como o token é consumido.
 */
@ExtendWith(MockitoExtension.class)
class RecuperacaoSenhaServiceTest {
    private static final String TOKEN = "token-de-teste";

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private TokenRedefinicaoSenhaRepository tokenRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private RecuperacaoSenhaService service;

    @BeforeEach
    void configurarPropriedades() {
        // Os campos @Value nao sao preenchidos fora do contexto do Spring.
        ReflectionTestUtils.setField(service, "validadeMinutos", 30L);
        ReflectionTestUtils.setField(service, "limitePorHora", 3L);
    }

    @Test
    void emailInexistenteNaoGravaNemEnviaNada() {
        when(usuarioRepository.findByEmail("ninguem@example.invalid")).thenReturn(Optional.empty());

        service.solicitar("ninguem@example.invalid");

        verifyNoInteractions(tokenRepository, eventPublisher);
    }

    @Test
    void solicitarIgnoraEspacosAoRedorDoEmail() {
        when(usuarioRepository.findByEmail("aluno@example.invalid")).thenReturn(Optional.empty());

        service.solicitar("  aluno@example.invalid  ");

        verify(usuarioRepository).findByEmail("aluno@example.invalid");
    }

    @Test
    void limiteDePedidosPorHoraAtingidoNaoGeraNovoTokenNemEmail() {
        when(usuarioRepository.findByEmail("aluno@example.invalid")).thenReturn(Optional.of(usuario()));
        when(tokenRepository.countByUsuarioIdAndCriadoEmAfter(eq(1L), any())).thenReturn(3L);

        service.solicitar("aluno@example.invalid");

        verify(tokenRepository, never()).invalidarPendentes(any(), any());
        verify(tokenRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void pedidoAbaixoDoLimiteAindaEAceito() {
        when(usuarioRepository.findByEmail("aluno@example.invalid")).thenReturn(Optional.of(usuario()));
        when(tokenRepository.countByUsuarioIdAndCriadoEmAfter(eq(1L), any())).thenReturn(2L);

        service.solicitar("aluno@example.invalid");

        verify(tokenRepository).save(any(TokenRedefinicaoSenha.class));
        verify(eventPublisher).publishEvent(any(RecuperacaoSenhaSolicitadaEvent.class));
    }

    @Test
    void novoPedidoInvalidaOsTokensPendentesAntesDeGravarONovo() {
        when(usuarioRepository.findByEmail("aluno@example.invalid")).thenReturn(Optional.of(usuario()));

        service.solicitar("aluno@example.invalid");

        InOrder ordem = inOrder(tokenRepository);
        ordem.verify(tokenRepository).invalidarPendentes(eq(1L), any(OffsetDateTime.class));
        ordem.verify(tokenRepository).save(any(TokenRedefinicaoSenha.class));
    }

    @Test
    void gravaSomenteOHashDoTokenEEnviaOTokenEmClaroNoEvento() throws Exception {
        when(usuarioRepository.findByEmail("aluno@example.invalid")).thenReturn(Optional.of(usuario()));
        OffsetDateTime antes = OffsetDateTime.now();

        service.solicitar("aluno@example.invalid");

        ArgumentCaptor<TokenRedefinicaoSenha> gravado = ArgumentCaptor.forClass(TokenRedefinicaoSenha.class);
        verify(tokenRepository).save(gravado.capture());
        ArgumentCaptor<RecuperacaoSenhaSolicitadaEvent> evento =
                ArgumentCaptor.forClass(RecuperacaoSenhaSolicitadaEvent.class);
        verify(eventPublisher).publishEvent(evento.capture());

        String token = evento.getValue().token();
        // 32 bytes em Base64 URL-safe sem padding ocupam 43 caracteres.
        assertTrue(token.matches("[A-Za-z0-9_-]{43}"));
        assertEquals(sha256(token), gravado.getValue().getTokenHash());
        assertTrue(gravado.getValue().getTokenHash().matches("[0-9a-f]{64}"));
        assertNotEquals(token, gravado.getValue().getTokenHash());
        assertNull(gravado.getValue().getUsadoEm());
        assertEquals(1L, gravado.getValue().getUsuario().getId());
        assertFalse(gravado.getValue().getExpiraEm().isBefore(antes.plusMinutes(30)));
        assertFalse(gravado.getValue().getExpiraEm().isAfter(OffsetDateTime.now().plusMinutes(30)));
        assertEquals("aluno@example.invalid", evento.getValue().email());
        assertEquals("Aluno Teste", evento.getValue().nome());
        assertEquals(30L, evento.getValue().validadeMinutos());
    }

    @Test
    void cadaPedidoGeraUmTokenDiferente() {
        when(usuarioRepository.findByEmail("aluno@example.invalid")).thenReturn(Optional.of(usuario()));

        service.solicitar("aluno@example.invalid");
        service.solicitar("aluno@example.invalid");

        ArgumentCaptor<RecuperacaoSenhaSolicitadaEvent> eventos =
                ArgumentCaptor.forClass(RecuperacaoSenhaSolicitadaEvent.class);
        verify(eventPublisher, times(2)).publishEvent(eventos.capture());
        assertNotEquals(eventos.getAllValues().get(0).token(), eventos.getAllValues().get(1).token());
    }

    @Test
    void tokenInexistenteExpiradoEUsadoRetornamAMesmaExcecao() throws Exception {
        when(tokenRepository.findByTokenHash(sha256("inexistente"))).thenReturn(Optional.empty());
        when(tokenRepository.findByTokenHash(sha256("expirado")))
                .thenReturn(Optional.of(registro(OffsetDateTime.now().minusMinutes(1), null)));
        when(tokenRepository.findByTokenHash(sha256("usado")))
                .thenReturn(Optional.of(registro(OffsetDateTime.now().plusMinutes(10), OffsetDateTime.now().minusMinutes(1))));

        LinkRedefinicaoInvalidoException semRegistro = assertThrows(LinkRedefinicaoInvalidoException.class,
                () -> service.redefinir(new RedefinirSenhaRequestDTO("inexistente", "novaSenha123")));
        LinkRedefinicaoInvalidoException vencido = assertThrows(LinkRedefinicaoInvalidoException.class,
                () -> service.redefinir(new RedefinirSenhaRequestDTO("expirado", "novaSenha123")));
        LinkRedefinicaoInvalidoException jaUsado = assertThrows(LinkRedefinicaoInvalidoException.class,
                () -> service.redefinir(new RedefinirSenhaRequestDTO("usado", "novaSenha123")));

        // Quem tenta adivinhar tokens nao pode distinguir os tres casos pela mensagem.
        assertEquals("Link inválido ou expirado.", semRegistro.getMessage());
        assertEquals(semRegistro.getMessage(), vencido.getMessage());
        assertEquals(semRegistro.getMessage(), jaUsado.getMessage());
        verify(tokenRepository, never()).invalidarPendentes(any(), any());
        verifyNoInteractions(passwordEncoder, eventPublisher);
    }

    @Test
    void tokenConsumidoPorOutraRequisicaoNaoAlteraASenha() throws Exception {
        when(tokenRepository.findByTokenHash(sha256(TOKEN)))
                .thenReturn(Optional.of(registro(OffsetDateTime.now().plusMinutes(10), null)));
        // Zero linhas atualizadas: outra requisicao concorrente ja consumiu este token.
        when(tokenRepository.invalidarPendentes(eq(1L), any())).thenReturn(0);

        assertThrows(LinkRedefinicaoInvalidoException.class,
                () -> service.redefinir(new RedefinirSenhaRequestDTO(TOKEN, "novaSenha123")));

        verifyNoInteractions(passwordEncoder, eventPublisher);
        verify(usuarioRepository, never()).findById(any());
    }

    @Test
    void redefinicaoValidaTrocaASenhaZeraAFlagEAvisaODono() throws Exception {
        Usuario usuario = usuario();
        usuario.setSenha("hash-antigo");
        usuario.setTrocarSenhaNoLogin(true);
        TokenRedefinicaoSenha registro = registro(OffsetDateTime.now().plusMinutes(10), null);
        registro.setUsuario(usuario);
        when(tokenRepository.findByTokenHash(sha256(TOKEN))).thenReturn(Optional.of(registro));
        when(tokenRepository.invalidarPendentes(eq(1L), any())).thenReturn(2);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.encode("novaSenha123")).thenReturn("hash-novo");

        service.redefinir(new RedefinirSenhaRequestDTO(TOKEN, "novaSenha123"));

        assertEquals("hash-novo", usuario.getSenha());
        assertFalse(usuario.getTrocarSenhaNoLogin());
        // Consumir/invalidar vem antes de trocar a senha: a requisicao perdedora de uma disputa nao altera nada.
        InOrder ordem = inOrder(tokenRepository, passwordEncoder);
        ordem.verify(tokenRepository).invalidarPendentes(eq(1L), any(OffsetDateTime.class));
        ordem.verify(passwordEncoder).encode("novaSenha123");
        ArgumentCaptor<SenhaRedefinidaEvent> evento = ArgumentCaptor.forClass(SenhaRedefinidaEvent.class);
        verify(eventPublisher).publishEvent(evento.capture());
        assertEquals("aluno@example.invalid", evento.getValue().email());
        assertEquals("Aluno Teste", evento.getValue().nome());
    }

    private Usuario usuario() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNome("Aluno Teste");
        usuario.setEmail("aluno@example.invalid");
        return usuario;
    }

    private TokenRedefinicaoSenha registro(OffsetDateTime expiraEm, OffsetDateTime usadoEm) {
        TokenRedefinicaoSenha registro = new TokenRedefinicaoSenha();
        registro.setUsuario(usuario());
        registro.setExpiraEm(expiraEm);
        registro.setUsadoEm(usadoEm);
        return registro;
    }

    private String sha256(String valor) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8)));
    }
}
