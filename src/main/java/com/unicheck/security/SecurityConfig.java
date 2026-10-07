package com.unicheck.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Configura JWT proprio e sessao stateless; a chave deve ser sobrescrita por variavel de ambiente em producao.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    private final SecretKey chave;
    private final String issuer;

    public SecurityConfig(@Value("${security.jwt.secret}") String segredo,
                          @Value("${security.jwt.issuer}") String issuer) {
        if (segredo.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("security.jwt.secret deve possuir ao menos 32 bytes");
        }
        this.chave = new SecretKeySpec(segredo.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
        this.issuer = issuer;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        // BCrypt inclui salt e custo de trabalho, portanto senhas iguais nao geram o mesmo hash.
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(UsuarioDetailsService detailsService,
                                                PasswordEncoder passwordEncoder) {
        // O provider concentra a comparacao BCrypt; a AuthService nao deve comparar senha manualmente.
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(detailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider::authenticate;
    }

    @Bean
    JwtEncoder jwtEncoder() {
        // Encoder e decoder usam a mesma chave porque o MVP assina tokens simetricos com HS256.
        return new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(chave));
    }

    @Bean
    JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(chave).macAlgorithm(MacAlgorithm.HS256).build();
        // Alem da assinatura e expiracao, o issuer impede aceitar tokens de outro emissor que use a mesma chave.
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
        return decoder;
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        // A claim guarda somente ADMIN/ALUNO; o prefixo e aplicado aqui para hasRole funcionar no Spring.
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("role");
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationConverter converter,
                                            TrocaSenhaObrigatoriaFilter trocaSenhaFilter,
                                            SecurityProblemHandler problemHandler) throws Exception {
        // JWT e reenviado a cada request, logo criar sessao no servidor so introduziria estado desnecessario.
        return http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Login e cadastro iniciam a jornada sem token; Swagger fica aberto somente para documentacao do MVP.
                        .requestMatchers("/auth/login", "/auth/cadastro", "/auth/esqueci-senha", "/auth/redefinir-senha", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth
                        .authenticationEntryPoint(problemHandler)
                        .accessDeniedHandler(problemHandler)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(converter)))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(problemHandler).accessDeniedHandler(problemHandler))
                .addFilterAfter(trocaSenhaFilter, BearerTokenAuthenticationFilter.class)
                .build();
    }
}
