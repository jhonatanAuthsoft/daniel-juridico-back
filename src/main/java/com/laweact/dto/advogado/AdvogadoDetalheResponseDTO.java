package com.laweact.dto.advogado;

import java.util.List;

import com.laweact.dto.cliente.EnderecoResponseDTO;

import lombok.Builder;

@Builder
public record AdvogadoDetalheResponseDTO(
        AdvogadoPerfilResponseDTO perfil,
        EnderecoResponseDTO endereco,
        List<OabResponseDTO> oabs,
        List<AreaAtuacaoResponseDTO> areasAtuacao,
        List<CatalogoItemResponseDTO> modalidades,
        List<EspecialidadeResponseDTO> especialidades,
        List<CatalogoItemResponseDTO> formasCobranca,
        List<PosGraduacaoResponseDTO> posGraduacoes
) {}
