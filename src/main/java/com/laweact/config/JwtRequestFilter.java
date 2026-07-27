package com.laweact.config;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.laweact.model.entity.UsuarioEntity;
import com.laweact.repository.TokenRevogadoRepository;
import com.laweact.repository.UsuarioRepository;
import com.laweact.service.imp.UsuarioDetailsServiceImp;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
@RequiredArgsConstructor
public class JwtRequestFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final TokenRevogadoRepository tokenRevogadoRepository;
    private final UsuarioDetailsServiceImp userDetailsServiceImp;
    private final UsuarioRepository usuarioRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        final String authorizationHeader = request.getHeader("Authorization");
        String username = null;
        String jwt = null;

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);

            if (tokenRevogadoRepository.existsByToken(jwt)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token expired. Please log in again.");
                return;
            }
            try {
                username = jwtUtil.extractEmail(jwt);
            } catch (Exception e) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
                return;
            }

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                try {
                    if (tokenInvalidadoPorResetSenha(username, jwt)) {
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token expired. Please log in again.");
                        return;
                    }

                    UserDetails userDetails = userDetailsServiceImp.loadUserByUsername(username);
                    if (jwtUtil.validateToken(jwt, userDetails)) {
                        UsernamePasswordAuthenticationToken authenticationToken =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails, null, userDetails.getAuthorities());
                        authenticationToken.setDetails(
                                new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                    }
                } catch (Exception e) {
                    log.info("Token inválido: {}", e.getMessage());
                }
            }
        }

        chain.doFilter(request, response);
    }

    private boolean tokenInvalidadoPorResetSenha(String email, String jwt) {
        return usuarioRepository.findByEmail(email)
                .map(UsuarioEntity::getTokensInvalidosAntes)
                .map(invalidosAntes -> {
                    Date issuedAt = jwtUtil.extractIssuedAt(jwt);
                    if (issuedAt == null) {
                        return false;
                    }
                    LocalDateTime iat = LocalDateTime.ofInstant(
                            Instant.ofEpochMilli(issuedAt.getTime()),
                            ZoneId.systemDefault()
                    );
                    return !iat.isAfter(invalidosAntes);
                })
                .orElse(false);
    }
}
