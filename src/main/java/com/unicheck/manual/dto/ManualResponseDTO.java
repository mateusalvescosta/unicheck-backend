package com.unicheck.manual.dto;

import java.util.List;

import com.unicheck.manual.enumeration.CategoriaManual;

/**
 * @apiNote Conteúdo completo da página do manual. Blocos opcionais vêm null
 *          (textos) ou vazios (listas), e o frontend exibe só os preenchidos.
 *
 *          "anterior" e "proximo" não são gravados: saem da ordem do manual na
 *          sequência, e vêm null no primeiro e no último.
 * @param id                    --> Identificador do manual.
 * @param ordem                 --> Posição do manual na sequência.
 * @param categoria             --> Constante da categoria.
 * @param categoriaNome         --> Nome da categoria, como exibido na página.
 * @param tempoLeitura          --> Tempo estimado de leitura, em minutos.
 * @param titulo                --> Título do manual.
 * @param resumo                --> Frase abaixo do título.
 * @param linkTexto             --> Texto do botão de link externo.
 * @param linkUrl               --> Endereço do botão de link externo.
 * @param quandoSeAplica        --> Bloco "Quando isso se aplica".
 * @param oQueSaber             --> Parágrafo "O que você precisa saber".
 * @param atencao               --> Itens da caixa "Atenção".
 * @param antesDeComecar        --> Itens de "Antes de começar".
 * @param passos                --> Passos de "Como fazer", já ordenados.
 * @param ondeResolver          --> Chips de "Onde resolver".
 * @param prazoERegras          --> Itens de "Prazo e regras".
 * @param resultadoEsperado     --> Bloco "Resultado esperado".
 * @param detalhes              --> Itens do "O que você precisa saber" recolhível.
 * @param entendaMelhor         --> Itens de "Entenda melhor".
 * @param proximoPassoDescricao --> Texto de "Seu próximo passo".
 * @param proximoPassoManual    --> Manual de "Ver orientação", se houver.
 * @param avisoAtualizacao      --> Texto de "Informação sujeita a atualização".
 * @param relacionados          --> Manuais de "Você também pode precisar".
 * @param anterior              --> "Conteúdo anterior" do rodapé.
 * @param proximo               --> "Próximo conteúdo" do rodapé.
 * @author Mateus Alves Costa
 * @since 27.09.2026
 */
public record ManualResponseDTO(
    Long id,
    Integer ordem,
    CategoriaManual categoria,
    String categoriaNome,
    Integer tempoLeitura,
    String titulo,
    String resumo,
    String linkTexto,
    String linkUrl,
    String quandoSeAplica,
    String oQueSaber,
    List<String> atencao,
    List<String> antesDeComecar,
    List<PassoManualDTO> passos,
    List<LocalAtendimentoDTO> ondeResolver,
    List<String> prazoERegras,
    String resultadoEsperado,
    List<String> detalhes,
    List<String> entendaMelhor,
    String proximoPassoDescricao,
    ManualReferenciaDTO proximoPassoManual,
    String avisoAtualizacao,
    List<ManualReferenciaDTO> relacionados,
    ManualReferenciaDTO anterior,
    ManualReferenciaDTO proximo
) {
}
