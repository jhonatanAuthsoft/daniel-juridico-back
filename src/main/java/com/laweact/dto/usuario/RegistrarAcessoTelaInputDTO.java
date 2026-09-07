package com.laweact.dto.usuario;

import com.laweact.model.enums.TelaAcessoEnum;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record RegistrarAcessoTelaInputDTO(
        @NotNull(message = "A tela acessada é obrigatória")
        TelaAcessoEnum tela
) {}
