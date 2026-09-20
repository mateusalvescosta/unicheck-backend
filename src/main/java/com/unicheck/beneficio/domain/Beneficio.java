package com.unicheck.beneficio.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.annotations.BatchSize;

import com.unicheck.beneficio.enumeration.TipoBeneficio;
import com.unicheck.categoria.domain.Categoria;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @apiNote Entidade que representa um benefício, mapeada na tabela
 *          "tb_beneficio".
 *
 *          Palavras-chave e passos são ElementCollection porque não existem fora
 *          do benefício: são lidos, salvos e removidos junto com ele. Já a
 *          categoria é entidade própria, e este é o lado dono do ManyToMany.
 *
 *          O equals e o hashCode usam apenas o id, para que a entidade se
 *          comporte bem dentro dos Set que ela mesma carrega.
 * @author Mateus Alves Costa
 * @since 03.08.2026
 * @updated 19.09.2026
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "tb_beneficio")
public class Beneficio {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "icone_url", length = 255)
    private String iconeUrl;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, length = 150)
    private String chamada;

    @Column(nullable = false, length = 255)
    private String descricao;

    @Column(nullable = false, length = 255)
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoBeneficio tipo;

    @Column(name = "publico_elegivel", nullable = false, length = 255)
    private String publicoElegivel;

    @Column(name = "observacao_elegibilidade", length = 500)
    private String observacaoElegibilidade;

    @Column(name = "aviso_importante", length = 255)
    private String avisoImportante;

    @Column(length = 100)
    private String disponibilidade;

    @BatchSize(size = 30)
    @ElementCollection
    @CollectionTable(name = "tb_beneficio_palavra_chave",
        joinColumns = @JoinColumn(name = "beneficio_id"))
    @Column(name = "palavra_chave", nullable = false, length = 60)
    private Set<String> palavrasChave = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "tb_beneficio_passo",
        joinColumns = @JoinColumn(name = "beneficio_id"))
    @OrderBy("ordem")
    private List<PassoBeneficio> passos = new ArrayList<>();

    @BatchSize(size = 30)
    @ManyToMany
    @JoinTable(name = "tb_beneficio_categoria",
        joinColumns = @JoinColumn(name = "beneficio_id"),
        inverseJoinColumns = @JoinColumn(name = "categoria_id"))
    private Set<Categoria> categorias = new HashSet<>();

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Beneficio other = (Beneficio) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }
}
