package com.shivamprogramming.chat_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // ✅ Disable CSRF — required for WebSocket/STOMP
            .csrf(AbstractHttpConfigurer::disable)

            // ✅ Disable HTTP Basic Auth — removes the browser username/password popup
            .httpBasic(AbstractHttpConfigurer::disable)

            // ✅ Disable form login — no redirect to /login page
            .formLogin(AbstractHttpConfigurer::disable)

            .authorizeHttpRequests(auth -> auth
                // ✅ Permit static files (HTML client)
                .requestMatchers("/", "/index.html", "/*.html", "/*.js", "/*.css").permitAll()

                // ✅ Permit SockJS WebSocket handshake + all SockJS transport endpoints
                .requestMatchers("/ws/**").permitAll()

                // ✅ Permit STOMP messaging destinations
                .requestMatchers("/app/**").permitAll()
                .requestMatchers("/topic/**").permitAll()
                .requestMatchers("/queue/**").permitAll()
                .requestMatchers("/user/**").permitAll()

                // ✅ Permit Actuator
                .requestMatchers("/actuator/**").permitAll()

                // 🔒 Everything else requires authentication
                .anyRequest().authenticated()
            );

        return http.build();
    }
}
