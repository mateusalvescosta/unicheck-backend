package com.unicheck.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityScheme;
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
                .components(new Components().addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")
                        .description("Informe o JWT emitido por POST /auth/login.")))
                .info(new Info()
                        .title("UniCheck API")
                        .description("API da plataforma UniCheck. Conteúdo de etapas é armazenado em Markdown; "
                                + "o frontend deve sanitizar HTML e bloquear URLs inseguras antes de renderizar.")
                        .version("v1.0")
                        .contact(new Contact()
                                .name("Juan Pablo")));
    }
}
