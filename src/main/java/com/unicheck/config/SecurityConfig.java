package com.unicheck.config;

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
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

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

    // O valor padrao permite subir o MVP localmente; em producao a propriedade deve vir de segredo externo.
    public SecurityConfig(@Value("${security.jwt.secret:unicheck-chave-hs256-de-desenvolvimento-2026}") String segredo) {
        this.chave = new SecretKeySpec(segredo.getBytes(), "HmacSHA256");
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        // BCrypt inclui salt e custo de trabalho, portanto senhas iguais nao geram o mesmo hash.
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(com.unicheck.usuario.security.UsuarioDetailsService detailsService,
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
        return NimbusJwtDecoder.withSecretKey(chave).macAlgorithm(MacAlgorithm.HS256).build();
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
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationConverter converter) throws Exception {
        // JWT e reenviado a cada request, logo criar sessao no servidor so introduziria estado desnecessario.
        return http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Login e cadastro iniciam a jornada sem token; Swagger fica aberto somente para documentacao do MVP.
                        .requestMatchers("/auth/login", "/auth/cadastro", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(converter)))
                .build();
    }
}
