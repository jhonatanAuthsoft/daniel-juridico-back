package com.laweact.dto.advogado;

import java.time.LocalDate;
import java.util.List;

import com.laweact.model.enums.PronomeTratamentoEnum;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record CadastrarAdvogadoInputDTO(
        @NotBlank(message = "O nome completo é obrigatório")
        String nomeCompleto,

        String nomeSocial,

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

        @NotBlank(message = "O RG é obrigatório")
        String rg,

        @NotBlank(message = "O órgão emissor do RG é obrigatório")
        String rgOrgaoEmissor,

        @NotBlank(message = "A UF do RG é obrigatória")
        @Size(min = 2, max = 2, message = "A UF do RG deve ter 2 letras")
        String rgUf,

        @NotBlank(message = "O CPF é obrigatório")
        String cpf,

        @NotBlank(message = "O nome do pai é obrigatório")
        String nomePai,

        @NotBlank(message = "O nome da mãe é obrigatório")
        String nomeMae,

        @NotNull(message = "O pronome de tratamento é obrigatório")
        PronomeTratamentoEnum pronomeTratamento,

        @NotBlank(message = "O telefone é obrigatório")
        String telefone,

        String fotoUrl,

        @NotBlank(message = "A universidade é obrigatória")
        String universidade,

        @NotBlank(message = "O curso é obrigatório")
        String curso,

        @NotNull(message = "O ano de formação é obrigatório")
        @Min(value = 1950, message = "Ano de formação inválido")
        @Max(value = 2100, message = "Ano de formação inválido")
        Integer anoFormacao,

        @NotNull(message = "A data de início de atuação é obrigatória")
        @PastOrPresent(message = "A data de atuação deve ser no passado ou presente")
        LocalDate atuacaoDesde,

        String biografia,

        @NotBlank(message = "O CEP é obrigatório")
        @Pattern(regexp = "^\\d{5}-?\\d{3}$", message = "CEP inválido")
        String cep,

        @NotBlank(message = "O logradouro é obrigatório")
        String logradouro,

        @NotBlank(message = "O número do endereço é obrigatório")
        String numero,

        String complemento,

        @NotBlank(message = "O bairro é obrigatório")
        String bairro,

        @NotBlank(message = "A cidade é obrigatória")
        String cidade,

        @NotBlank(message = "O estado é obrigatório")
        @Size(min = 2, max = 2, message = "O estado deve ter 2 letras (UF)")
        String estado,

        @NotNull(message = "A OAB principal é obrigatória")
        @Valid
        OabInputDTO oabPrincipal,

        @Size(max = 5, message = "São permitidas no máximo 5 OABs suplementares")
        @Valid
        List<OabInputDTO> oabsSuplementares,

        @NotEmpty(message = "Informe ao menos uma área de atuação")
        @Valid
        List<AreaAtuacaoInputDTO> areasAtuacao,

        @NotEmpty(message = "Informe ao menos uma modalidade de atuação")
        List<String> modalidades,

        @Valid
        List<EspecialidadeInputDTO> especialidades,

        @NotEmpty(message = "Informe ao menos uma forma de cobrança")
        List<String> formasCobranca,

        @Valid
        List<PosGraduacaoInputDTO> posGraduacoes
) {}
