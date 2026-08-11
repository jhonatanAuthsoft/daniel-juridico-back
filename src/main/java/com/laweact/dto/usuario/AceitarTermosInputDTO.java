package com.laweact.dto.usuario;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record AceitarTermosInputDTO(
        @NotNull(message = "O aceite via checkbox é obrigatório")
        @AssertTrue(message = "É obrigatório marcar o aceite dos termos de uso")
        Boolean checkboxConfirmado,

        Boolean scrollConfirmado,

        String versao
) {}
