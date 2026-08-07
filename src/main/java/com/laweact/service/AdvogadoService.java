package com.laweact.service;

import java.util.UUID;

import com.laweact.dto.advogado.AdvogadoPerfilPublicoResponseDTO;
import com.laweact.dto.advogado.CadastrarAdvogadoInputDTO;
import com.laweact.dto.advogado.CadastrarAdvogadoResponseDTO;
import com.laweact.dto.avaliacao.AvaliacaoListagemResponseDTO;
import com.laweact.dto.shared.PaginationInfo;

public interface AdvogadoService {

    CadastrarAdvogadoResponseDTO cadastrar(CadastrarAdvogadoInputDTO input);

    AdvogadoPerfilPublicoResponseDTO obterPerfilPublico(UUID advogadoId);

    record AvaliacoesPaginadas(AvaliacaoListagemResponseDTO data, PaginationInfo pagination) {}

    AvaliacoesPaginadas listarAvaliacoes(UUID advogadoId, int limit, int offset);

    UUID excluirAvaliacao(UUID advogadoId, UUID avaliacaoId);
}
