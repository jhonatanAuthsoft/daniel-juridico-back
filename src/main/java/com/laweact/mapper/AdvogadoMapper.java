package com.laweact.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.laweact.dto.advogado.AdvogadoPerfilResponseDTO;
import com.laweact.dto.advogado.AreaAtuacaoResponseDTO;
import com.laweact.dto.advogado.CadastrarAdvogadoResponseDTO;
import com.laweact.dto.advogado.OabResponseDTO;
import com.laweact.dto.cliente.EnderecoResponseDTO;
import com.laweact.model.entity.AdvogadoEntity;
import com.laweact.model.entity.AreaAtuacaoAdvogadoEntity;
import com.laweact.model.entity.EnderecoEntity;
import com.laweact.model.entity.OabEntity;
import com.laweact.model.entity.UsuarioEntity;

@Component
public class AdvogadoMapper {

    private final UsuarioMapper usuarioMapper;
    private final ClienteMapper clienteMapper;

    public AdvogadoMapper(UsuarioMapper usuarioMapper, ClienteMapper clienteMapper) {
        this.usuarioMapper = usuarioMapper;
        this.clienteMapper = clienteMapper;
    }

    public AdvogadoPerfilResponseDTO toPerfilResponse(AdvogadoEntity advogado) {
        return AdvogadoPerfilResponseDTO.builder()
                .usuarioId(advogado.getUsuarioId())
                .nomeCompleto(advogado.getNomeCompleto())
                .nomeSocial(advogado.getNomeSocial())
                .rg(advogado.getRg())
                .rgOrgaoEmissor(advogado.getRgOrgaoEmissor())
                .rgUf(advogado.getRgUf())
                .cpf(advogado.getCpf())
                .nomePai(advogado.getNomePai())
                .nomeMae(advogado.getNomeMae())
                .pronomeTratamento(advogado.getPronomeTratamento())
                .fotoUrl(advogado.getFotoUrl())
                .universidade(advogado.getUniversidade())
                .curso(advogado.getCurso())
                .anoFormacao(advogado.getAnoFormacao())
                .atuacaoDesde(advogado.getAtuacaoDesde())
                .biografia(advogado.getBiografia())
                .disponibilidade(advogado.getDisponibilidade())
                .statusVerificacao(advogado.getStatusVerificacao())
                .mediaAvaliacoes(advogado.getMediaAvaliacoes())
                .totalAvaliacoes(advogado.getTotalAvaliacoes())
                .build();
    }

    public OabResponseDTO toOabResponse(OabEntity oab) {
        return OabResponseDTO.builder()
                .id(oab.getId())
                .numero(oab.getNumero())
                .uf(oab.getUf())
                .principal(oab.getPrincipal())
                .fotoFrenteUrl(oab.getFotoFrenteUrl())
                .fotoVersoUrl(oab.getFotoVersoUrl())
                .statusValidacao(oab.getStatusValidacao())
                .build();
    }

    public AreaAtuacaoResponseDTO toAreaResponse(AreaAtuacaoAdvogadoEntity area) {
        return AreaAtuacaoResponseDTO.builder()
                .id(area.getId())
                .estado(area.getEstado())
                .cidade(area.getCidade())
                .build();
    }

    public CadastrarAdvogadoResponseDTO toCadastrarResponse(
            UsuarioEntity usuario,
            AdvogadoEntity advogado,
            EnderecoEntity endereco,
            List<OabEntity> oabs,
            List<AreaAtuacaoAdvogadoEntity> areas,
            String token
    ) {
        EnderecoResponseDTO enderecoResponse = clienteMapper.toEnderecoResponse(endereco);
        return CadastrarAdvogadoResponseDTO.builder()
                .usuario(usuarioMapper.toResponseDTO(usuario))
                .advogado(toPerfilResponse(advogado))
                .endereco(enderecoResponse)
                .oabs(oabs.stream().map(this::toOabResponse).toList())
                .areasAtuacao(areas.stream().map(this::toAreaResponse).toList())
                .token(token)
                .build();
    }
}
