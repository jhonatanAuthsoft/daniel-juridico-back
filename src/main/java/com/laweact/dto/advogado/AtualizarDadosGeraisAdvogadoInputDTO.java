package com.laweact.dto.advogado;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import lombok.Builder;

@Builder
public record AtualizarDadosGeraisAdvogadoInputDTO(
        @NotBlank(message = "O nome é obrigatório")
        String nomeCompleto,

        @NotBlank(message = "O telefone é obrigatório")
        String telefone,

        @Past(message = "A data de nascimento deve ser no passado")
        LocalDate dataNascimento
) {}
