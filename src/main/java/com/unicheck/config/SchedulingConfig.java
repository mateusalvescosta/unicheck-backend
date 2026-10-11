package com.unicheck.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Habilita @Scheduled para a rotina diaria de lembretes de checklist parado. */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
