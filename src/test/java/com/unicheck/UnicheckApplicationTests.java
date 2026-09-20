package com.unicheck;

import com.unicheck.beneficio.repository.*;
import com.unicheck.categoria.repository.*;
import com.unicheck.checklist.repository.*;
import com.unicheck.gameficacao.repository.*;
import com.unicheck.usuario.repository.*;
import com.unicheck.usuario.domain.*;
import com.unicheck.usuario.enumeration.RoleUsuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Carrega a composição real da aplicação sem conectar ao banco de desenvolvimento. */
@SpringBootTest(properties = {
        "spring.data.jpa.repositories.enabled=false",
        "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration",
        "security.jwt.secret=0123456789abcdef0123456789abcdef",
        "security.jwt.issuer=https://teste.unicheck.local"
})
@AutoConfigureMockMvc
class UnicheckApplicationTests {
    @MockitoBean UsuarioRepository usuarioRepository;
    @MockitoBean AlunoRepository alunoRepository;
    @MockitoBean AdminRepository adminRepository;
    @MockitoBean ChecklistRepository checklistRepository;
    @MockitoBean ItemChecklistRepository itemChecklistRepository;
    @MockitoBean ProgressoItemRepository progressoItemRepository;
    @MockitoBean ChecklistConcluidoRepository checklistConcluidoRepository;
    @MockitoBean ChecklistPrerequisitoRepository checklistPrerequisitoRepository;
    @MockitoBean NivelRepository nivelRepository;
    @MockitoBean AlunoSeloRepository alunoSeloRepository;
    // Beneficio e categoria tambem entram no contexto real; sem o mock, BeneficioService
    // nao encontra dependencia e a aplicacao inteira deixa de subir neste teste.
    @MockitoBean BeneficioRepository beneficioRepository;
    @MockitoBean BeneficioFavoritoRepository beneficioFavoritoRepository;
    @MockitoBean CategoriaRepository categoriaRepository;
    @Autowired MockMvc mockMvc;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired ObjectMapper objectMapper;

    @BeforeEach
    void persistenciaMockadaAtribuiIds() {
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocacao -> {
            Usuario usuario = invocacao.getArgument(0); usuario.setId(100L); return usuario;
        });
        when(alunoRepository.save(any(Aluno.class))).thenAnswer(invocacao -> {
            Aluno aluno = invocacao.getArgument(0); aluno.setId(200L); aluno.setXpTotal(0); return aluno;
        });
    }

    @Test
    void contextoWebESegurancaInicializaSemBancoExterno() {
        // A inicialização do contexto é a própria asserção; persistência é excluída deliberadamente.
    }

    @Test
    void cadastroPublicoMantemContratoHttpSemPermitirEscolhaDeRole() throws Exception {
        mockMvc.perform(post("/auth/cadastro").contentType(MediaType.APPLICATION_JSON).content("""
                {"nome":"Aluno Teste","email":"aluno@example.invalid","ra":"RA-HTTP-1",
                 "dataNascimento":"2005-03-15","senha":"senha-teste-123","periodo":"1"}
                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/auth/cadastro/200"))
                .andExpect(jsonPath("$.usuario.role").value("ALUNO"));
    }

    @Test
    void loginAdminUsaEncoderEJwtReais() throws Exception {
        Usuario usuario = usuario("ADMIN-HTTP", "senha-admin-teste", RoleUsuario.ADMIN, false);
        Admin admin = new Admin(); admin.setId(9L); admin.setUsuario(usuario); admin.setCargo("TESTE");
        when(usuarioRepository.findByRa(usuario.getRa())).thenReturn(Optional.of(usuario));
        when(adminRepository.findByUsuarioId(usuario.getId())).thenReturn(Optional.of(admin));

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ra\":\"ADMIN-HTTP\",\"senha\":\"senha-admin-teste\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.trocarSenhaObrigatorio").value(false))
                .andExpect(jsonPath("$.token", not(blankOrNullString())));
    }

    @Test
    void tokenInvalidoRetornaProblemDetail401EAlunoRecebe403EmRotaAdmin() throws Exception {
        mockMvc.perform(get("/admin/checklists").header("Authorization", "Bearer token-adulterado"))
                .andExpect(status().isUnauthorized()).andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Falha na autenticacao"));

        Usuario aluno = usuario("ALUNO-HTTP", "senha-aluno-teste", RoleUsuario.ALUNO, false);
        when(usuarioRepository.findByRa(aluno.getRa())).thenReturn(Optional.of(aluno));
        String resposta = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ra\":\"ALUNO-HTTP\",\"senha\":\"senha-aluno-teste\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(resposta).get("token").asText();

        mockMvc.perform(get("/admin/checklists").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden()).andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void trocaObrigatoriaRestringeAcessoEAceitaNovoLogin() throws Exception {
        Usuario admin = usuario("ADMIN-TROCA", "senha-temporaria", RoleUsuario.ADMIN, true);
        Admin perfil = new Admin(); perfil.setId(10L); perfil.setUsuario(admin); perfil.setCargo("TESTE");
        when(usuarioRepository.findByRa(admin.getRa())).thenReturn(Optional.of(admin));
        when(adminRepository.findByUsuarioId(admin.getId())).thenReturn(Optional.of(perfil));
        String login = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ra\":\"ADMIN-TROCA\",\"senha\":\"senha-temporaria\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.trocarSenhaObrigatorio").value(true))
                .andReturn().getResponse().getContentAsString();
        String tokenRestrito = objectMapper.readTree(login).get("token").asText();

        mockMvc.perform(get("/admin/checklists").header("Authorization", "Bearer " + tokenRestrito))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.title").value("Troca de senha obrigatoria"));
        mockMvc.perform(patch("/usuarios/trocar-senha").header("Authorization", "Bearer " + tokenRestrito)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senhaAtual\":\"senha-temporaria\",\"novaSenha\":\"senha-nova-segura\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ra\":\"ADMIN-TROCA\",\"senha\":\"senha-temporaria\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ra\":\"ADMIN-TROCA\",\"senha\":\"senha-nova-segura\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.trocarSenhaObrigatorio").value(false));
    }

    private Usuario usuario(String ra, String senha, RoleUsuario role, boolean trocaObrigatoria) {
        Usuario usuario = new Usuario(); usuario.setId(1L); usuario.setRa(ra);
        usuario.setSenha(passwordEncoder.encode(senha)); usuario.setRole(role);
        usuario.setTrocarSenhaNoLogin(trocaObrigatoria); return usuario;
    }
}
