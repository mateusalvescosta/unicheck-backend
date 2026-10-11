package com.unicheck.notificacao.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Dispara uma vez por dia a verificacao de checklists parados. Com mais de uma instancia da
 * aplicacao, cada uma rodaria a rotina e duplicaria lembretes (ver pendencias da spec).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChecklistParadoJob {
    private final NotificacaoService notificacaoService;

    @Scheduled(cron = "${app.notificacao.checklist-parado.cron:0 0 9 * * *}",
            zone = "${app.notificacao.checklist-parado.zona:America/Sao_Paulo}")
    public void executar() {
        int gravadas = notificacaoService.notificarChecklistsParados(LocalDateTime.now());
        log.info("Lembretes de checklist parado gravados: {}", gravadas);
    }
}
