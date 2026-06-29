package com.shivamprogramming.chat_service.config;

import com.shivamprogramming.chat_service.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // ✅ Disable CSRF — required for WebSocket/STOMP
            .csrf(AbstractHttpConfigurer::disable)

            // ✅ CORS configuration
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // ✅ Disable HTTP Basic Auth — removes the browser username/password popup
            .httpBasic(AbstractHttpConfigurer::disable)

            // ✅ Disable form login — no redirect to /login page
            .formLogin(AbstractHttpConfigurer::disable)

            // ✅ Stateless session — JWT is used for auth
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

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

                // ✅ Permit auth endpoint (for standalone mode without external auth service)
                .requestMatchers("/api/chat/auth/**").permitAll()

                // ✅ Permit file uploads/downloads
                .requestMatchers("/api/chat/files/**").permitAll()

                // ✅ Permit Chat REST API (protected by JWT filter when token is present)
                .requestMatchers("/api/chat/**").permitAll()

                // 🔒 Everything else requires authentication
                .anyRequest().authenticated()
            )

            // ✅ Add JWT filter before UsernamePasswordAuthenticationFilter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
