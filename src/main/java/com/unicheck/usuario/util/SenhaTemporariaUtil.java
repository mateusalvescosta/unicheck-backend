package com.unicheck.usuario.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Gera a senha temporaria a partir da data de nascimento, no formato
 * ddMMyyyy (só numeros, sem barra) -- ex: 2005-03-15 vira "15032005".
 * Usado tanto no cadastro de Aluno quanto na criacao de Admin, por isso
 * ficou numa classe utilitaria separada em vez de repetir o codigo
 * nas duas Services.
 */
public class SenhaTemporariaUtil {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("ddMMyyyy");

    private SenhaTemporariaUtil() {
        // classe utilitaria, nao deve ser instanciada
    }

    public static String gerar(LocalDate dataNascimento) {
        return dataNascimento.format(FORMATO);
    }
}