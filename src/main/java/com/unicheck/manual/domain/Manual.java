package com.unicheck.manual.domain;

import java.util.ArrayList;
import java.util.List;

import com.unicheck.manual.enumeration.CategoriaManual;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @apiNote Entidade que representa uma página do Manual do Aluno, mapeada na
 *          tabela "tb_manual".
 *
 *          Só categoria, tempo de leitura, título, resumo e "O que você precisa
 *          saber" aparecem em todas as páginas. Os demais blocos são opcionais e
 *          o frontend exibe apenas os que vierem preenchidos; o mesmo vale para
 *          os atalhos do topo ("Antes de começar", "Como fazer", "Prazo e
 *          regras"). Os blocos em lista são ElementCollection porque não existem
 *          fora do manual.
 *
 *          A ordem define a sequência do manual: é a ordem das listagens e de
 *          onde saem o "Conteúdo anterior" e o "Próximo conteúdo" do rodapé, que
 *          por isso não são gravados. Os outros vínculos com a própria entidade
 *          são o manual indicado em "Seu próximo passo" e os até três manuais de
 *          "Você também pode precisar", cujo limite fica a cargo do DTO.
 *
 *          O equals e o hashCode usam apenas o id, para que a entidade se
 *          comporte bem nas coleções que ela mesma carrega.
 * @author Mateus Alves Costa
 * @since 27.09.2026
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "tb_manual")
public class Manual {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer ordem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private CategoriaManual categoria;

    @Column(name = "tempo_leitura", nullable = false)
    private Integer tempoLeitura;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(nullable = false, length = 255)
    private String resumo;

    @Column(name = "link_texto", length = 100)
    private String linkTexto;

    @Column(name = "link_url", length = 255)
    private String linkUrl;

    @Column(name = "quando_se_aplica", length = 500)
    private String quandoSeAplica;

    @Column(name = "o_que_saber", nullable = false, length = 1000)
    private String oQueSaber;

    @ElementCollection
    @CollectionTable(name = "tb_manual_atencao",
        joinColumns = @JoinColumn(name = "manual_id"))
    @OrderColumn(name = "ordem")
    @Column(name = "descricao", nullable = false, length = 500)
    private List<String> atencao = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "tb_manual_requisito",
        joinColumns = @JoinColumn(name = "manual_id"))
    @OrderColumn(name = "ordem")
    @Column(name = "descricao", nullable = false, length = 255)
    private List<String> antesDeComecar = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "tb_manual_passo",
        joinColumns = @JoinColumn(name = "manual_id"))
    @OrderBy("ordem")
    private List<PassoManual> passos = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "tb_manual_local",
        joinColumns = @JoinColumn(name = "manual_id"))
    @OrderColumn(name = "ordem")
    private List<LocalAtendimento> ondeResolver = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "tb_manual_prazo_regra",
        joinColumns = @JoinColumn(name = "manual_id"))
    @OrderColumn(name = "ordem")
    @Column(name = "descricao", nullable = false, length = 500)
    private List<String> prazoERegras = new ArrayList<>();

    @Column(name = "resultado_esperado", length = 255)
    private String resultadoEsperado;

    /**
     * @apiNote Itens do bloco recolhível "O que você precisa saber", que
     *          complementa o parágrafo de mesmo nome do topo da página.
     */
    @ElementCollection
    @CollectionTable(name = "tb_manual_detalhe",
        joinColumns = @JoinColumn(name = "manual_id"))
    @OrderColumn(name = "ordem")
    @Column(name = "descricao", nullable = false, length = 500)
    private List<String> detalhes = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "tb_manual_entenda_melhor",
        joinColumns = @JoinColumn(name = "manual_id"))
    @OrderColumn(name = "ordem")
    @Column(name = "descricao", nullable = false, length = 500)
    private List<String> entendaMelhor = new ArrayList<>();

    @Column(name = "proximo_passo_descricao", length = 255)
    private String proximoPassoDescricao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proximo_passo_manual_id")
    private Manual proximoPassoManual;

    @Column(name = "aviso_atualizacao", length = 255)
    private String avisoAtualizacao;

    @ManyToMany
    @JoinTable(name = "tb_manual_relacionado",
        joinColumns = @JoinColumn(name = "manual_id"),
        inverseJoinColumns = @JoinColumn(name = "relacionado_id"))
    @OrderColumn(name = "ordem")
    private List<Manual> relacionados = new ArrayList<>();

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
        Manual other = (Manual) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }
}
