package com.unicheck.beneficio.enumeration;

/**
 * @apiNote Tipos de benefício usados como filtro na listagem e exibidos no card.
 *          O nome da constante é gravado como texto na coluna "tipo" de
 *          tb_beneficio, portanto renomear uma constante exige migration.
 * @author Mateus Alves Costa
 * @since 18.08.2026
 * @updated 19.09.2026
 */
public enum TipoBeneficio {
    GRATUITO,
    DESCONTO,
    PRECO_EDUCACIONAL,
    PROGRAMA_ESTUDANTIL,
    DEPENDE_INSTITUICAO,
    DIREITO_PUBLICO
}
