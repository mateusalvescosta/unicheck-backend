package com.unicheck.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração da documentação automática da API (Swagger/OpenAPI).
 *
 * Gera a tela interativa em /swagger-ui.html, onde qualquer dev do time
 * consegue ver todos os endpoints disponíveis, os formatos de request/
 * response, e testar chamadas direto pelo navegador — sem precisar de
 * Postman nem ler o código dos controllers.
 *
 * Não precisa mexer aqui pra documentar endpoints novos: o springdoc
 * lê as anotações dos Controllers automaticamente. Essa classe só
 * define metadados gerais (título, versão, contato) que aparecem no
 * topo da página do Swagger.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI unicheckOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("UniCheck API")
                        .description("API da plataforma UniCheck — checklists, gamificação e analytics para calouros")
                        .version("v1.0")
                        .contact(new Contact()
                                .name("Juan Pablo")));
    }
}