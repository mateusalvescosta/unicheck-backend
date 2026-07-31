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

    @OneToOne(mappedBy = "usuario")
    private Aluno aluno;

    @OneToOne(mappedBy = "usuario")
    private Admin admin;

    @PrePersist
    protected void onCreate() {
        this.criadoEm = OffsetDateTime.now();
    }
}
