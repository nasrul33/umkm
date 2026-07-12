package com.siaumkm.config;

import com.siaumkm.auth.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

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
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // API stateless berbasis JWT; SPA menangani CSRF via token header
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Dispatch internal ke halaman error Boot (bukan akses /error langsung
                // dari luar — dispatch REQUEST tetap kena denyAll di bawah).
                .dispatcherTypeMatchers(jakarta.servlet.DispatcherType.ERROR).permitAll()
                // Modul A — Halaman Profil Bisnis (SRS-A-01..07): publik, tanpa autentikasi
                .requestMatchers("/public/**", "/actuator/health").permitAll()
                // Pintu masuk login SPA — satu-satunya endpoint /app tanpa token
                .requestMatchers("/app/auth/login").permitAll()
                // Modul B1-B6 — Aplikasi Akuntansi: wajib autentikasi
                .requestMatchers("/app/**").authenticated()
                .anyRequest().denyAll()
            )
            // API stateless: tanpa token yang valid balas 401, bukan 403 default
            .exceptionHandling(ex -> ex.authenticationEntryPoint(
                (request, response, authException) ->
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED)))
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
