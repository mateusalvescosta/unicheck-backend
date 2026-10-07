package com.unicheck.auth.repository;

import com.unicheck.auth.domain.TokenRedefinicaoSenha;
import com.unicheck.usuario.domain.Usuario;
import com.unicheck.usuario.enumeration.RoleUsuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @apiNote Exercita as queries de TokenRedefinicaoSenhaRepository contra o Postgres de verdade
 *          (busca por hash, contagem por janela e invalidação em massa), incluindo a migration V17.
 *
 *          Cada teste roda em transação revertida e todo fixture usa o prefixo "zzt-". A classe é
 *          ignorada quando não há Postgres na porta esperada, como as demais de repositório.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnabledIf("bancoDisponivel")
class TokenRedefinicaoSenhaRepositoryTest {
    @Autowired TokenRedefinicaoSenhaRepository repository;
    @Autowired TestEntityManager em;

    private Usuario dono;
    private Usuario outro;

    static boolean bancoDisponivel() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", 5432), 500);
            return true;
        } catch (Exception naoDisponivel) {
            return false;
        }
    }

    @BeforeEach
    void semear() {
        dono = em.persist(usuario("zzt-dono"));
        outro = em.persist(usuario("zzt-outro"));
    }

    @Test
    void findByTokenHashEncontraSomenteOHashExato() {
        TokenRedefinicaoSenha salvo = em.persistAndFlush(token(dono, "zzt-hash-1", null));

        assertEquals(salvo.getId(), repository.findByTokenHash("zzt-hash-1").orElseThrow().getId());
        assertTrue(repository.findByTokenHash("zzt-hash-2").isEmpty());
    }

    @Test
    void hashDuplicadoViolaAConstraintDeUnicidade() {
        em.persistAndFlush(token(dono, "zzt-hash-unico", null));

        assertThrows(Exception.class, () -> em.persistAndFlush(token(outro, "zzt-hash-unico", null)));
    }

    @Test
    void contagemConsideraSomenteOsTokensDoUsuarioDentroDaJanela() {
        em.persist(token(dono, "zzt-c1", null));
        em.persist(token(dono, "zzt-c2", OffsetDateTime.now()));
        em.persist(token(outro, "zzt-c3", null));
        em.flush();

        // Tokens ja usados tambem contam: o limite e sobre pedidos, nao sobre links ainda validos.
        assertEquals(2, repository.countByUsuarioIdAndCriadoEmAfter(dono.getId(), OffsetDateTime.now().minusHours(1)));
        assertEquals(1, repository.countByUsuarioIdAndCriadoEmAfter(outro.getId(), OffsetDateTime.now().minusHours(1)));
        assertEquals(0, repository.countByUsuarioIdAndCriadoEmAfter(dono.getId(), OffsetDateTime.now().plusHours(1)));
    }

    @Test
    void invalidarPendentesMarcaSoOsPendentesDoUsuario() {
        OffsetDateTime jaUsadoEm = OffsetDateTime.now().minusMinutes(5);
        TokenRedefinicaoSenha pendente1 = em.persist(token(dono, "zzt-i1", null));
        TokenRedefinicaoSenha pendente2 = em.persist(token(dono, "zzt-i2", null));
        TokenRedefinicaoSenha usado = em.persist(token(dono, "zzt-i3", jaUsadoEm));
        TokenRedefinicaoSenha deOutro = em.persist(token(outro, "zzt-i4", null));
        em.flush();

        OffsetDateTime agora = OffsetDateTime.now();
        int atualizados = repository.invalidarPendentes(dono.getId(), agora);

        assertEquals(2, atualizados);
        // A query limpa o contexto de persistencia, entao os valores vem de novo do banco.
        assertNotNull(repository.findById(pendente1.getId()).orElseThrow().getUsadoEm());
        assertNotNull(repository.findById(pendente2.getId()).orElseThrow().getUsadoEm());
        assertEquals(jaUsadoEm.toInstant().toEpochMilli(),
                repository.findById(usado.getId()).orElseThrow().getUsadoEm().toInstant().toEpochMilli());
        assertNull(repository.findById(deOutro.getId()).orElseThrow().getUsadoEm());
        // Sem pendentes restantes, uma segunda chamada nao atualiza nada: e isso que protege contra uso duplo.
        assertEquals(0, repository.invalidarPendentes(dono.getId(), OffsetDateTime.now()));
    }

    private Usuario usuario(String ra) {
        Usuario usuario = new Usuario();
        usuario.setNome("Teste " + ra);
        usuario.setRa(ra);
        usuario.setSenha("hash-qualquer");
        usuario.setDataNascimento(LocalDate.of(2000, 1, 1));
        usuario.setRole(RoleUsuario.ALUNO);
        usuario.setEmail(ra + "@example.invalid");
        return usuario;
    }

    private TokenRedefinicaoSenha token(Usuario usuario, String hash, OffsetDateTime usadoEm) {
        TokenRedefinicaoSenha token = new TokenRedefinicaoSenha();
        token.setUsuario(usuario);
        token.setTokenHash(hash);
        token.setExpiraEm(OffsetDateTime.now().plusMinutes(30));
        token.setUsadoEm(usadoEm);
        return token;
    }
}
