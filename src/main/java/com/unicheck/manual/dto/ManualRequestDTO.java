package com.unicheck.manual.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.unicheck.manual.enumeration.CategoriaManual;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * @apiNote Corpo de cadastro e de atualização do manual.
 *
 *          Diferente do benefício, a atualização também é completa: o
 *          formulário do admin envia o manual inteiro, e campo opcional ausente
 *          significa "remover". A única exceção é a ordem, que ausente mantém a
 *          posição atual (ou, no cadastro, coloca o manual no fim da sequência).
 * @param ordem                 --> Posição na sequência; opcional.
 * @param categoria             --> Categoria do manual; obrigatória.
 * @param tempoLeitura          --> Tempo de leitura em minutos; obrigatório.
 * @param titulo                --> Título; obrigatório e único.
 * @param resumo                --> Frase abaixo do título; obrigatória.
 * @param linkTexto             --> Texto do botão de link; vem junto com linkUrl.
 * @param linkUrl               --> Endereço do botão de link; vem junto com linkTexto.
 * @param quandoSeAplica        --> Bloco "Quando isso se aplica"; opcional.
 * @param oQueSaber             --> Parágrafo "O que você precisa saber"; obrigatório.
 * @param atencao               --> Itens da caixa "Atenção"; opcional.
 * @param antesDeComecar        --> Itens de "Antes de começar"; opcional.
 * @param passos                --> Passos de "Como fazer"; opcional.
 * @param ondeResolver          --> Chips de "Onde resolver"; opcional.
 * @param prazoERegras          --> Itens de "Prazo e regras"; opcional.
 * @param resultadoEsperado     --> Bloco "Resultado esperado"; opcional.
 * @param detalhes              --> Itens do "O que você precisa saber" recolhível; opcional.
 * @param entendaMelhor         --> Itens de "Entenda melhor"; opcional.
 * @param proximoPassoDescricao --> Texto de "Seu próximo passo"; obrigatório se
 *                              houver proximoPassoManualId.
 * @param proximoPassoManualId  --> Manual de "Ver orientação"; opcional.
 * @param avisoAtualizacao      --> Texto de "Informação sujeita a atualização"; opcional.
 * @param relacionadoIds        --> Até três manuais de "Você também pode precisar",
 *                              na ordem de exibição.
 * @author Mateus Alves Costa
 * @since 27.09.2026
 */
public record ManualRequestDTO(
    @Positive (message = "A ordem do manual deve ser maior que zero")
    Integer ordem,
    @NotNull (message = "A categoria do manual é obrigatória")
    CategoriaManual categoria,
    @NotNull (message = "O tempo de leitura é obrigatório")
    @Positive (message = "O tempo de leitura deve ser maior que zero")
    Integer tempoLeitura,
    @NotBlank (message = "O título do manual é obrigatório")
    @Size (max = 150, message = "O título deve ter no máximo 150 caracteres")
    String titulo,
    @NotBlank (message = "O resumo do manual é obrigatório")
    @Size (max = 255, message = "O resumo deve ter no máximo 255 caracteres")
    String resumo,
    @Size (max = 100, message = "O texto do link deve ter no máximo 100 caracteres")
    String linkTexto,
    @Size (max = 255, message = "A URL do link deve ter no máximo 255 caracteres")
    String linkUrl,
    @Size (max = 500, message = "\"Quando isso se aplica\" deve ter no máximo 500 caracteres")
    String quandoSeAplica,
    @NotBlank (message = "\"O que você precisa saber\" é obrigatório")
    @Size (max = 1000, message = "\"O que você precisa saber\" deve ter no máximo 1000 caracteres")
    String oQueSaber,
    List<@NotBlank @Size(max = 500) String> atencao,
    List<@NotBlank @Size(max = 255) String> antesDeComecar,
    List<@NotNull @Valid PassoManualDTO> passos,
    List<@NotNull @Valid LocalAtendimentoDTO> ondeResolver,
    List<@NotBlank @Size(max = 500) String> prazoERegras,
    @Size (max = 255, message = "O resultado esperado deve ter no máximo 255 caracteres")
    String resultadoEsperado,
    List<@NotBlank @Size(max = 500) String> detalhes,
    List<@NotBlank @Size(max = 500) String> entendaMelhor,
    @Size (max = 255, message = "O texto do próximo passo deve ter no máximo 255 caracteres")
    String proximoPassoDescricao,
    Long proximoPassoManualId,
    @Size (max = 255, message = "O aviso de atualização deve ter no máximo 255 caracteres")
    String avisoAtualizacao,
    @Size (max = 3, message = "O manual pode ter no máximo 3 relacionados")
    List<@NotNull Long> relacionadoIds
) {

    /**
     * @apiNote O botão de link precisa de texto e endereço; um sem o outro não
     *          tem como ser exibido.
     * @return --> Retorna true quando os dois vêm preenchidos ou os dois vazios.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @JsonIgnore
    @AssertTrue (message = "O texto e a URL do link devem ser informados juntos")
    public boolean isLinkCompleto() {
        return preenchido(linkTexto) == preenchido(linkUrl);
    }

    /**
     * @apiNote "Ver orientação" fica dentro do bloco "Seu próximo passo", que não
     *          é exibido sem texto.
     * @return --> Retorna true quando não há manual indicado ou quando há texto.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @JsonIgnore
    @AssertTrue (message = "O texto do próximo passo é obrigatório quando há manual indicado")
    public boolean isProximoPassoCompleto() {
        return proximoPassoManualId == null || preenchido(proximoPassoDescricao);
    }

    private static boolean preenchido(String valor) {
        return valor != null && !valor.isBlank();
    }
}
