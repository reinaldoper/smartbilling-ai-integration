package br.com.reinaldodev.smartbilling.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Perfil local didático. Não use esta configuração aberta em produção.
 * Ative o perfil oauth2 para validar JWT emitido por um Authorization Server.
 */
@Configuration
public class SecurityConfig {

    @Bean
    @Profile("!oauth2")
    SecurityFilterChain localSecurity(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .anyRequest().permitAll())
                .build();
    }

    @Bean
    @Profile("oauth2")
    SecurityFilterChain oauth2Security(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .build();
    }
}
