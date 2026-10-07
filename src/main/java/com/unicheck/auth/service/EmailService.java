package com.unicheck.auth.service;

import com.unicheck.auth.dto.RecuperacaoSenhaSolicitadaEvent;
import com.unicheck.auth.dto.SenhaRedefinidaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    @Value("${app.mail.remetente}")
    private String remetente;
    @Value("${app.recuperacao-senha.url-frontend}")
    private String urlFrontend;

    /** Roda depois do commit (garante que o token ja existe) e em outra thread (o tempo de resposta nao revela se o e-mail existe). */
    @Async
    @TransactionalEventListener
    public void enviarRecuperacao(RecuperacaoSenhaSolicitadaEvent evento) {
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(remetente);
        mensagem.setTo(evento.email());
        mensagem.setSubject("Unicheck: redefinição de senha");
        // O token vai no fragmento (#): fragmentos nao sao enviados ao servidor nem aparecem em logs ou no Referer.
        mensagem.setText("Olá, " + evento.nome() + "!\n\n"
                + "Recebemos um pedido para redefinir a senha da sua conta no Unicheck. "
                + "Para escolher uma nova senha, acesse o link abaixo:\n\n"
                + urlFrontend + "#token=" + evento.token() + "\n\n"
                + "O link vale por " + evento.validadeMinutos() + " minutos e só pode ser usado uma vez.\n\n"
                + "Se você não pediu essa redefinição, ignore este e-mail: sua senha continua a mesma.");
        try {
            mailSender.send(mensagem);
        } catch (MailException e) {
            // Nem o token nem o endereco entram no log; a causa detalhada pode conter o destinatario.
            log.error("Falha ao enviar e-mail de recuperacao de senha: {}", e.getClass().getSimpleName());
        }
    }

    /** Avisa o dono da conta, para ele perceber uma redefinicao que nao fez. */
    @Async
    @TransactionalEventListener
    public void enviarConfirmacao(SenhaRedefinidaEvent evento) {
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(remetente);
        mensagem.setTo(evento.email());
        mensagem.setSubject("Unicheck: sua senha foi alterada");
        mensagem.setText("Olá, " + evento.nome() + "!\n\n"
                + "A senha da sua conta no Unicheck foi alterada em "
                + evento.alteradaEm().format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm")) + ".\n\n"
                + "Se foi você, não precisa fazer nada. Se não reconhece essa alteração, "
                + "entre em contato com o suporte da instituição imediatamente.");
        try {
            mailSender.send(mensagem);
        } catch (MailException e) {
            log.error("Falha ao enviar e-mail de confirmacao de senha alterada: {}", e.getClass().getSimpleName());
        }
    }
}
