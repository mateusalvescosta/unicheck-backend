package com.unicheck.usuario.domain;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import com.unicheck.usuario.enumeration.RoleUsuario;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Setter
@Getter
@Entity
@Table(name = "tb_usuario")
@AllArgsConstructor
@NoArgsConstructor
public class Usuario implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String nome;

    @Column(nullable = false, unique = true, length = 25)
    private String ra;

    @Column(nullable = false, length = 255)
    private String senha;

    @Column(name = "data_nascimento", nullable = false)
    private LocalDate dataNascimento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoleUsuario role;

    @Column(name = "foto_url", length = 255)
    private String fotoUrl;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    // NOVO: controla se esse usuário precisa trocar a senha no próximo login.
    // true = senha temporária (definida por outra pessoa, ex: Admin criando outro Admin)
    // false = o próprio usuário escolheu a senha (cadastro público de Aluno, ou já trocou)
    @Column(name = "trocar_senha_no_login", nullable = false)
    private Boolean trocarSenhaNoLogin;

    @PrePersist
    protected void onCreate() {
        this.criadoEm = OffsetDateTime.now();
        if (this.trocarSenhaNoLogin == null) {
            this.trocarSenhaNoLogin = false; // default: não precisa trocar
        }
    }

    /**
     * O RA e o identificador de login comum para alunos e administradores.
     */
    @Override
    public String getUsername() {
        return ra;
    }

    /**
     * Explicito para o contrato UserDetails; o valor persistido ja e o hash BCrypt.
     */
    @Override
    public String getPassword() {
        return senha;
    }

    /**
     * A role e unica no dominio, portanto nao ha necessidade de uma tabela N:N.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
