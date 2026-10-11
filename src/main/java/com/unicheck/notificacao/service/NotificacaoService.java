package com.unicheck.notificacao.service;

import com.unicheck.notificacao.enumeration.TipoNotificacao;
import com.unicheck.notificacao.repository.NotificacaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificacaoService {
    private final NotificacaoRepository notificacaoRepository;

    @Value("${app.notificacao.checklist-parado.dias-para-primeiro:3}")
    private int diasParaPrimeiro;
    @Value("${app.notificacao.checklist-parado.dias-entre-lembretes:7}")
    private int diasEntreLembretes;
    @Value("${app.notificacao.retencao.dias-lidas:30}")
    private int diasRetencaoLidas;
    @Value("${app.notificacao.retencao.dias-nao-lidas:90}")
    private int diasRetencaoNaoLidas;

    /**
     * Grava uma notificacao para cada aluno que nao desligou o tipo. Roda na transacao de quem
     * cria o conteudo: se a criacao falhar, nenhuma notificacao fica apontando para algo que nao existe.
     */
    @Transactional
    public void notificarAlunos(TipoNotificacao tipo, Long referenciaId, String mensagem) {
        notificacaoRepository.notificarAlunos(tipo.name(), referenciaId, mensagem);
    }

    /**
     * Recebe o instante em vez de ler o relogio, para os testes simularem dias sem esperar.
     * Usa LocalDateTime porque as datas de progresso ficam em colunas TIMESTAMP sem fuso,
     * gravadas no horario local da aplicacao.
     */
    @Transactional
    public int notificarChecklistsParados(LocalDateTime agora) {
        return notificacaoRepository.notificarChecklistsParados(agora, diasParaPrimeiro, diasEntreLembretes);
    }

    /** Recebe o instante pelo mesmo motivo de notificarChecklistsParados: os testes simulam a passagem dos dias. */
    @Transactional
    public int apagarAntigas(LocalDateTime agora) {
        return notificacaoRepository.apagarAntigas(agora, diasRetencaoLidas, diasRetencaoNaoLidas);
    }

    /** Chamado na exclusao do objeto, antes de apaga-lo, para nenhuma notificacao levar a algo inexistente. */
    @Transactional
    public void removerDoObjeto(Long referenciaId, TipoNotificacao... tipos) {
        notificacaoRepository.removerDoObjeto(List.of(tipos), referenciaId);
    }
}
