package com.unicheck.notificacao.domain;

import java.time.OffsetDateTime;

import com.unicheck.notificacao.enumeration.TipoNotificacao;
import com.unicheck.usuario.domain.Usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "tb_notificacao")
public class Notificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoNotificacao tipo;

    // Sem FK: aponta para beneficio, checklist ou manual conforme o tipo. Ao excluir o objeto,
    // a Service dele apaga as notificacoes correspondentes.
    @Column(name = "referencia_id", nullable = false)
    private Long referenciaId;

    @Column(nullable = false, length = 255)
    private String mensagem;

    @Column(nullable = false)
    private Boolean lida;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    @PrePersist
    protected void onCreate() {
        this.lida = false;
        this.criadoEm = OffsetDateTime.now();
    }
}
