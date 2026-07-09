package com.chessdigitizer.backend.infrastructure.adapter.in.security;

import com.chessdigitizer.backend.domain.port.out.TokenIssuer;
import com.chessdigitizer.backend.infrastructure.adapter.out.security.CurrentUserContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final TokenIssuer tokenIssuer;

    public TokenAuthenticationFilter(TokenIssuer tokenIssuer) {
        this.tokenIssuer = tokenIssuer;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String header = request.getHeader(AUTH_HEADER);
            if (header != null && header.startsWith(BEARER_PREFIX)) {
                String token = header.substring(BEARER_PREFIX.length());
                tokenIssuer.resolveUserId(token).ifPresent(CurrentUserContextHolder::set);
            }
            filterChain.doFilter(request, response);
        } finally {
            CurrentUserContextHolder.clear();   // crítico: evita fugas entre peticiones en el pool de hilos
        }
    }
}