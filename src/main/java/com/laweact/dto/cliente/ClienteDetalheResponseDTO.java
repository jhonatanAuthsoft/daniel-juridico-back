package com.laweact.dto.cliente;

import lombok.Builder;

@Builder
public record ClienteDetalheResponseDTO(
        ClientePerfilResponseDTO perfil,
        EnderecoResponseDTO endereco
) {}
