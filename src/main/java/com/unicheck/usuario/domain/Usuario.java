package com.unicheck.usuario.domain;

import java.time.OffsetDateTime;

import com.unicheck.usuario.enumeration.RoleUsuario;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "tb_usuario")
@AllArgsConstructor
@NoArgsConstructor
public class Usuario {
    
    @Id
    @GeneratedValue(strategy  = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String nome;

    @Column(nullable = false, length = 255)
    private String senha;

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
        if (this.trocarSenhaNoLogin ==  null) {
            this.trocarSenhaNoLogin = false; // default: não precisa trocar
        }
    }
}
