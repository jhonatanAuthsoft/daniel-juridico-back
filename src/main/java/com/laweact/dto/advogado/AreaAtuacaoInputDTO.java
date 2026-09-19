package com.laweact.dto.advogado;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record AreaAtuacaoInputDTO(
        @NotBlank(message = "O estado da área de atuação é obrigatório")
        @Size(min = 2, max = 2, message = "O estado deve ter 2 letras (UF)")
        String estado,

        String cidade,

        Boolean todoEstado
) {
    public boolean cobreTodoEstado() {
        return Boolean.TRUE.equals(todoEstado);
    }

    @AssertTrue(message = "A cidade da área de atuação é obrigatória")
    public boolean isCidadeInformadaQuandoNaoTodoEstado() {
        return cobreTodoEstado() || (cidade != null && !cidade.isBlank());
    }
}
