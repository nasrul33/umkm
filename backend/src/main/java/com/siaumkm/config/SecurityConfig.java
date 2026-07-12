package com.siaumkm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * SRS-A-05: Halaman publik (/public/**) dan aplikasi akuntansi (/app/**) dipisah
 * tegas di level filter chain. Jangan pernah menyatukan kedua filter chain ini —
 * lihat CLAUDE.md Aturan Emas #7.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // API stateless berbasis JWT; SPA menangani CSRF via token header
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Modul A — Halaman Profil Bisnis (SRS-A-01..07): publik, tanpa autentikasi
                .requestMatchers("/public/**", "/actuator/health").permitAll()
                // Modul B1-B6 — Aplikasi Akuntansi: wajib autentikasi
                .requestMatchers("/app/**").authenticated()
                .anyRequest().denyAll()
            );
        // TODO: tambahkan JwtAuthenticationFilter di sini sebelum UsernamePasswordAuthenticationFilter
        return http.build();
    }
}
