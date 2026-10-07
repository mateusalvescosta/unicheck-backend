package com.unicheck.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** Habilita @Async para o envio de e-mails nao atrasar a resposta das rotas publicas de recuperacao de senha. */
@Configuration
@EnableAsync
public class AsyncConfig {
}
