package com.laweact.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laweact.dto.shared.ApiError;
import com.laweact.dto.shared.ApiResponse;
import com.laweact.model.entity.AssinaturaEntity;
import com.laweact.model.entity.UsuarioEntity;
import com.laweact.model.enums.PerfilUsuarioEnum;
import com.laweact.repository.AssinaturaRepository;
import com.laweact.repository.UsuarioRepository;
import com.laweact.service.AssinaturaAcessoService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Component
@RequiredArgsConstructor
@Log4j2
public class AssinaturaInterceptor implements HandlerInterceptor {

    private static final Set<String> ALLOWLIST_PREFIXES = Set.of(
            "/usuarios/me",
            "/usuarios/logout",
            "/usuarios/excluir-conta",
            "/assinaturas",
            "/arquivos",
            "/dev/assinaturas"
    );

    private final UsuarioRepository usuarioRepository;
    private final AssinaturaRepository assinaturaRepository;
    private final AssinaturaAcessoService assinaturaAcessoService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if (!"GET".equalsIgnoreCase(request.getMethod())
                && !"POST".equalsIgnoreCase(request.getMethod())
                && !"PUT".equalsIgnoreCase(request.getMethod())
                && !"PATCH".equalsIgnoreCase(request.getMethod())
                && !"DELETE".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();
        if (isAllowlisted(path)) {
            return true;
        }

        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof UserDetails userDetails)) {
            return true;
        }

        UsuarioEntity usuario = usuarioRepository.findByEmail(userDetails.getUsername()).orElse(null);
        if (usuario == null || usuario.getPerfil() != PerfilUsuarioEnum.ADVOGADO) {
            return true;
        }

        AssinaturaEntity assinatura = assinaturaRepository.findByUsuario_Id(usuario.getId()).orElse(null);
        if (assinaturaAcessoService.isAcessoLiberado(usuario, assinatura)) {
            return true;
        }

        log.info("Acesso bloqueado por assinatura: path={} user={}", path, userDetails.getUsername());
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiResponse<Void> body = ApiResponse.error(
                List.of(ApiError.builder()
                        .code("SUBSCRIPTION_REQUIRED")
                        .detail("Assinatura ativa ou período de testes válido é necessário")
                        .build()),
                "Assinatura necessária para continuar"
        );
        response.getWriter().write(objectMapper.writeValueAsString(body));
        return false;
    }

    private boolean isAllowlisted(String path) {
        return ALLOWLIST_PREFIXES.stream().anyMatch(path::startsWith);
    }
}
