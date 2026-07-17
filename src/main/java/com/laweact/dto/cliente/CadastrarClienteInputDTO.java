package com.laweact.dto.cliente;

import java.time.LocalDate;

import com.laweact.model.enums.PronomesEnum;
import com.laweact.model.enums.TipoDocumentoEnum;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record CadastrarClienteInputDTO(
        @NotBlank(message = "O nome completo é obrigatório")
        String nomeCompleto,

        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "O e-mail deve ser válido")
        String email,

        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 8, message = "A senha deve ter no mínimo 8 caracteres")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
                message = "A senha deve conter ao menos 1 letra maiúscula, 1 minúscula e 1 número"
        )
        String senha,

        @NotBlank(message = "A profissão é obrigatória")
        String profissao,

        @NotNull(message = "O tipo de documento é obrigatório")
        TipoDocumentoEnum tipoDocumento,

        @NotBlank(message = "O número do documento é obrigatório")
        String numeroDocumento,

        @NotBlank(message = "O RG é obrigatório")
        String rg,

        @NotNull(message = "A data de nascimento é obrigatória")
        @Past(message = "A data de nascimento deve ser no passado")
        LocalDate dataNascimento,

        @NotNull(message = "Os pronomes são obrigatórios")
        PronomesEnum pronomes,

        @NotBlank(message = "O telefone é obrigatório")
        String telefone,

        @NotBlank(message = "O CEP é obrigatório")
        @Pattern(regexp = "^\\d{5}-?\\d{3}$", message = "CEP inválido")
        String cep,

        @NotBlank(message = "O logradouro é obrigatório")
        String logradouro,

        @NotBlank(message = "O número do endereço é obrigatório")
        String numero,

        @NotBlank(message = "O bairro é obrigatório")
        String bairro,

        @NotBlank(message = "A cidade é obrigatória")
        String cidade,

        @NotBlank(message = "O estado é obrigatório")
        @Size(min = 2, max = 2, message = "O estado deve ter 2 letras (UF)")
        String estado,

        String fotoUrl,
        String faixaRenda,
        String estadoCivil,

        @NotNull(message = "O aceite dos termos é obrigatório")
        Boolean aceiteTermos
) {}
