package com.unicheck.manual.enumeration;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @apiNote Assuntos do Manual do Aluno, exibidos em "Encontre por assunto", nos
 *          chips de filtro e acima do título de cada manual. A ordem de
 *          declaração é a ordem em que a tela os apresenta.
 *
 *          O nome da constante é gravado como texto na coluna "categoria" de
 *          tb_manual, portanto renomear ou acrescentar uma constante exige
 *          migration (a coluna tem CHECK com os valores aceitos).
 * @author Mateus Alves Costa
 * @since 27.09.2026
 */
@Getter
@RequiredArgsConstructor
public enum CategoriaManual {
    MATRICULA_VIDA_ACADEMICA("Matrícula e vida acadêmica", "Vida acadêmica",
            "Matrícula, RA, histórico, trancamento e procedimentos acadêmicos."),
    PORTAL_FERRAMENTAS_DIGITAIS("Portal e ferramentas digitais", "Ferramentas",
            "Portal Acadêmico, Meu EduCONNECT, AVA, e-mail e Microsoft Teams."),
    UTILIDADES_INSTITUCIONAIS("Utilidades institucionais", "Utilidades",
            "Calendário, documentos, serviços, contatos e informações rápidas."),
    BIBLIOTECA_ESTRUTURA_CAMPUS("Biblioteca e estrutura do campus", "Campus",
            "Bibliotecas, internet, laboratórios, clínicas e espaços do campus."),
    SERVICOS_APOIO_ESTUDANTE("Serviços e apoio ao estudante", "Apoio",
            "Multiatendimento, coordenação, Ouvidoria e núcleos de apoio."),
    AVALIACOES_NOTAS_FREQUENCIA("Avaliações, notas e frequência", "Avaliações",
            "Avaliação, aprovação, faltas, revisão e situações acadêmicas especiais."),
    FORMACAO_CARREIRA_ACADEMICA("Formação e carreira acadêmica", "Formação",
            "Projetos, estágio, TCC, ENADE, egressos e colação de grau."),
    COMECANDO_UNISALES("Começando no UniSales", "Começando",
            "Instituição, proposta educacional, metodologia e organização acadêmica.");

    private final String nome;
    private final String nomeCurto;
    private final String descricao;
}
