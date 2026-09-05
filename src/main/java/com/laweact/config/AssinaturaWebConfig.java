package com.laweact.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class AssinaturaWebConfig implements WebMvcConfigurer {

    private final AssinaturaInterceptor assinaturaInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(assinaturaInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/usuarios/login",
                        "/usuarios/refresh",
                        "/usuarios/recuperar-senha",
                        "/usuarios/validar-codigo-recuperacao",
                        "/usuarios/redefinir-senha",
                        "/usuarios/email-disponivel",
                        "/clientes/cadastrar",
                        "/advogados/cadastrar",
                        "/catalogos/**",
                        "/jobs/**",
                        "/assinaturas/notificacoes/**",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/actuator/**"
                );
    }
}
