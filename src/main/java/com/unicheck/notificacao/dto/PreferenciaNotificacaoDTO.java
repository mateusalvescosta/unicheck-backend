package com.unicheck.notificacao.dto;

import com.unicheck.notificacao.enumeration.TipoNotificacao;

public record PreferenciaNotificacaoDTO(TipoNotificacao tipo, boolean ativo) {
}
