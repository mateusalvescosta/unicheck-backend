package com.unicheck.notificacao.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** Apaga uma vez por dia as notificações que passaram do prazo de retenção (lidas e não lidas). */
@Slf4j
@Component
@RequiredArgsConstructor
public class LimpezaNotificacaoJob {
    private final NotificacaoService notificacaoService;

    @Scheduled(cron = "${app.notificacao.retencao.cron:0 0 3 * * *}",
            zone = "${app.notificacao.retencao.zona:America/Sao_Paulo}")
    public void executar() {
        int apagadas = notificacaoService.apagarAntigas(LocalDateTime.now());
        log.info("Notificacoes antigas apagadas: {}", apagadas);
    }
}
