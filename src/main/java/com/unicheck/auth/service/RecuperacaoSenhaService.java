package com.unicheck.auth.service;

import com.unicheck.auth.domain.TokenRedefinicaoSenha;
import com.unicheck.auth.dto.RecuperacaoSenhaSolicitadaEvent;
import com.unicheck.auth.dto.RedefinirSenhaRequestDTO;
import com.unicheck.auth.dto.SenhaRedefinidaEvent;
import com.unicheck.auth.repository.TokenRedefinicaoSenhaRepository;
import com.unicheck.exception.LinkRedefinicaoInvalidoException;
import com.unicheck.usuario.domain.Usuario;
import com.unicheck.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RecuperacaoSenhaService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UsuarioRepository usuarioRepository;
    private final TokenRedefinicaoSenhaRepository tokenRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.recuperacao-senha.validade-minutos:30}")
    private long validadeMinutos;
    @Value("${app.recuperacao-senha.limite-por-hora:3}")
    private long limitePorHora;

    @Transactional
    public void solicitar(String email) {
        // Todo caminho que nao envia retorna em silencio: o controller responde 202 igual e nao revela se o e-mail existe.
        Optional<Usuario> encontrado = usuarioRepository.findByEmail(email.trim());
        if (encontrado.isEmpty()) {
            return;
        }
        Usuario usuario = encontrado.get();
        OffsetDateTime agora = OffsetDateTime.now();
        if (tokenRepository.countByUsuarioIdAndCriadoEmAfter(usuario.getId(), agora.minusHours(1)) >= limitePorHora) {
            return;
        }
        // Um novo pedido substitui os links anteriores que ainda estavam validos.
        tokenRepository.invalidarPendentes(usuario.getId(), agora);

        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        // SHA-256 basta porque o token ja tem 256 bits de entropia; o valor em claro nunca e persistido.
        String tokenHash;
        try {
            tokenHash = HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponivel", e);
        }

        TokenRedefinicaoSenha registro = new TokenRedefinicaoSenha();
        registro.setUsuario(usuario);
        registro.setTokenHash(tokenHash);
        registro.setExpiraEm(agora.plusMinutes(validadeMinutos));
        tokenRepository.save(registro);

        eventPublisher.publishEvent(
                new RecuperacaoSenhaSolicitadaEvent(usuario.getNome(), usuario.getEmail(), token, validadeMinutos));
    }

    @Transactional
    public void redefinir(RedefinirSenhaRequestDTO dto) {
        String tokenHash;
        try {
            tokenHash = HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(dto.token().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponivel", e);
        }
        // Inexistente, usado ou expirado lancam a mesma excecao: quem tenta adivinhar tokens nao descobre a causa.
        TokenRedefinicaoSenha registro = tokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(LinkRedefinicaoInvalidoException::new);
        OffsetDateTime agora = OffsetDateTime.now();
        if (registro.getUsadoEm() != null || registro.getExpiraEm().isBefore(agora)) {
            throw new LinkRedefinicaoInvalidoException();
        }
        Long usuarioId = registro.getUsuario().getId();
        // Consome este token e invalida os demais pendentes numa unica instrucao. Se duas requisicoes
        // concorrentes usarem o mesmo link, so uma encontra o token ainda pendente; a outra recebe 0 linhas.
        if (tokenRepository.invalidarPendentes(usuarioId, agora) == 0) {
            throw new LinkRedefinicaoInvalidoException();
        }
        // O update em massa limpa o contexto de persistencia, entao o usuario e recarregado antes de alterar a senha.
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(LinkRedefinicaoInvalidoException::new);
        usuario.setSenha(passwordEncoder.encode(dto.novaSenha()));
        usuario.setTrocarSenhaNoLogin(false);
        eventPublisher.publishEvent(new SenhaRedefinidaEvent(usuario.getNome(), usuario.getEmail(), agora));
    }
}
