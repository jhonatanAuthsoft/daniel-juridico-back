package com.laweact.e2e.support;

import java.time.LocalDate;
import java.util.List;

import com.laweact.dto.advogado.AreaAtuacaoInputDTO;
import com.laweact.dto.advogado.CadastrarAdvogadoInputDTO;
import com.laweact.dto.advogado.OabInputDTO;
import com.laweact.dto.cliente.CadastrarClienteInputDTO;
import com.laweact.model.enums.PronomeTratamentoEnum;
import com.laweact.model.enums.PronomesEnum;
import com.laweact.model.enums.TipoDocumentoEnum;

public final class Fixtures {

    public static final String VALID_PASSWORD = "Secret12";

    private Fixtures() {
    }

    public static CadastrarClienteInputDTO clienteValido(String email, String documento) {
        return CadastrarClienteInputDTO.builder()
                .nomeCompleto("Maria Silva")
                .email(email)
                .senha(VALID_PASSWORD)
                .profissao("Analista")
                .tipoDocumento(TipoDocumentoEnum.CPF)
                .numeroDocumento(documento)
                .rg("1234567")
                .dataNascimento(LocalDate.of(1990, 5, 20))
                .pronomes(PronomesEnum.ELA)
                .telefone("11999999999")
                .cep("01310-100")
                .logradouro("Av. Paulista")
                .numero("1000")
                .bairro("Bela Vista")
                .cidade("São Paulo")
                .estado("SP")
                .aceiteTermos(true)
                .build();
    }

    public static CadastrarClienteInputDTO clienteCom(String email, String senha, String documento) {
        return CadastrarClienteInputDTO.builder()
                .nomeCompleto("Maria Silva")
                .email(email)
                .senha(senha)
                .profissao("Analista")
                .tipoDocumento(TipoDocumentoEnum.CPF)
                .numeroDocumento(documento)
                .rg("1234567")
                .dataNascimento(LocalDate.of(1990, 5, 20))
                .pronomes(PronomesEnum.ELA)
                .telefone("11999999999")
                .cep("01310-100")
                .logradouro("Av. Paulista")
                .numero("1000")
                .bairro("Bela Vista")
                .cidade("São Paulo")
                .estado("SP")
                .aceiteTermos(true)
                .build();
    }

    public static CadastrarAdvogadoInputDTO advogadoValido(String email, String cpf, String oabNumero) {
        return CadastrarAdvogadoInputDTO.builder()
                .nomeCompleto("João Advogado")
                .email(email)
                .senha(VALID_PASSWORD)
                .rg("7654321")
                .rgOrgaoEmissor("SSP")
                .rgUf("SP")
                .cpf(cpf)
                .nomePai("José Advogado")
                .nomeMae("Ana Advogada")
                .pronomeTratamento(PronomeTratamentoEnum.DOUTOR)
                .telefone("11988887777")
                .universidade("USP")
                .curso("Direito")
                .anoFormacao(2015)
                .atuacaoDesde(LocalDate.of(2016, 1, 10))
                .cep("01310-100")
                .logradouro("Av. Paulista")
                .numero("1500")
                .bairro("Bela Vista")
                .cidade("São Paulo")
                .estado("SP")
                .oabPrincipal(OabInputDTO.builder().numero(oabNumero).uf("SP").build())
                .areasAtuacao(List.of(AreaAtuacaoInputDTO.builder().estado("SP").cidade("São Paulo").build()))
                .aceiteTermos(true)
                .build();
    }
}
