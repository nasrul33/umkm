package com.siaumkm.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Membaca header Authorization: Bearer <jwt> dan mengisi SecurityContext.
 * Principal = UUID app_user.id (dipakai @AuthenticationPrincipal di controller),
 * authority = ROLE_OWNER / ROLE_STAFF (dipakai @PreAuthorize — NFR-10).
 * Token tidak valid: lanjut tanpa autentikasi, biarkan filter chain menolak.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            jwtService.verifikasi(header.substring(BEARER_PREFIX.length())).ifPresent(payload -> {
                var auth = new UsernamePasswordAuthenticationToken(
                        payload.userId(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + payload.peran().name())));
                SecurityContextHolder.getContext().setAuthentication(auth);
            });
        }
        filterChain.doFilter(request, response);
    }
}
