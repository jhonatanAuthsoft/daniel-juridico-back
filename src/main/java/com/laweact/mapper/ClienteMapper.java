package com.laweact.mapper;

import org.springframework.stereotype.Component;

import com.laweact.dto.cliente.CadastrarClienteResponseDTO;
import com.laweact.dto.cliente.ClienteDetalheResponseDTO;
import com.laweact.dto.cliente.ClientePerfilResponseDTO;
import com.laweact.dto.cliente.EnderecoResponseDTO;
import com.laweact.dto.usuario.UsuarioResponseDTO;
import com.laweact.model.entity.ClienteEntity;
import com.laweact.model.entity.EnderecoEntity;
import com.laweact.model.entity.UsuarioEntity;

@Component
public class ClienteMapper {

    private final UsuarioMapper usuarioMapper;

    public ClienteMapper(UsuarioMapper usuarioMapper) {
        this.usuarioMapper = usuarioMapper;
    }

    public ClientePerfilResponseDTO toPerfilResponse(ClienteEntity cliente) {
        return ClientePerfilResponseDTO.builder()
                .usuarioId(cliente.getUsuarioId())
                .nomeCompleto(cliente.getNomeCompleto())
                .razaoSocial(cliente.getRazaoSocial())
                .areaAtuacao(cliente.getAreaAtuacao())
                .profissao(cliente.getProfissao())
                .tipoDocumento(cliente.getTipoDocumento())
                .numeroDocumento(cliente.getNumeroDocumento())
                .rg(cliente.getRg())
                .dataNascimento(cliente.getDataNascimento())
                .pronomes(cliente.getPronomes())
                .fotoUrl(cliente.getFotoUrl())
                .faixaRenda(cliente.getFaixaRenda())
                .estadoCivil(cliente.getEstadoCivil())
                .build();
    }

    public EnderecoResponseDTO toEnderecoResponse(EnderecoEntity endereco) {
        return EnderecoResponseDTO.builder()
                .id(endereco.getId())
                .cep(endereco.getCep())
                .logradouro(endereco.getLogradouro())
                .numero(endereco.getNumero())
                .complemento(endereco.getComplemento())
                .bairro(endereco.getBairro())
                .cidade(endereco.getCidade())
                .estado(endereco.getEstado())
                .build();
    }

    public ClienteDetalheResponseDTO toDetalheResponse(ClienteEntity cliente, EnderecoEntity endereco) {
        return ClienteDetalheResponseDTO.builder()
                .perfil(toPerfilResponse(cliente))
                .endereco(endereco != null ? toEnderecoResponse(endereco) : null)
                .build();
    }

    public CadastrarClienteResponseDTO toCadastrarResponse(
            UsuarioEntity usuario,
            ClienteEntity cliente,
            EnderecoEntity endereco,
            String token
    ) {
        UsuarioResponseDTO usuarioResponse = usuarioMapper.toResponseDTO(usuario);
        return CadastrarClienteResponseDTO.builder()
                .usuario(usuarioResponse)
                .cliente(toPerfilResponse(cliente))
                .endereco(toEnderecoResponse(endereco))
                .token(token)
                .build();
    }
}
