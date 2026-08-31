package com.laweact.service;

import java.util.UUID;

import com.laweact.dto.advogado.AdvogadoDetalheResponseDTO;
import com.laweact.dto.advogado.AdvogadoPerfilPublicoResponseDTO;
import com.laweact.dto.advogado.AtualizarBiografiaAdvogadoInputDTO;
import com.laweact.dto.advogado.AtualizarDadosGeraisAdvogadoInputDTO;
import com.laweact.dto.advogado.AtualizarDisponibilidadeAdvogadoInputDTO;
import com.laweact.dto.advogado.AtualizarDocumentacaoAdvogadoInputDTO;
import com.laweact.dto.advogado.AtualizarEnderecoAdvogadoInputDTO;
import com.laweact.dto.advogado.AtualizarFormasCobrancaAdvogadoInputDTO;
import com.laweact.dto.advogado.AtualizarGraduacaoAdvogadoInputDTO;
import com.laweact.dto.advogado.CadastrarAdvogadoInputDTO;
import com.laweact.dto.advogado.CadastrarAdvogadoResponseDTO;
import com.laweact.dto.avaliacao.AvaliacaoItemResponseDTO;
import com.laweact.dto.avaliacao.AvaliacaoListagemResponseDTO;
import com.laweact.dto.avaliacao.CriarAvaliacaoInputDTO;
import com.laweact.dto.shared.PaginationInfo;

public interface AdvogadoService {

    CadastrarAdvogadoResponseDTO cadastrar(CadastrarAdvogadoInputDTO input);

    AdvogadoDetalheResponseDTO atualizarDadosGerais(AtualizarDadosGeraisAdvogadoInputDTO input);

    AdvogadoDetalheResponseDTO atualizarEndereco(AtualizarEnderecoAdvogadoInputDTO input);

    AdvogadoDetalheResponseDTO atualizarFormasCobranca(AtualizarFormasCobrancaAdvogadoInputDTO input);

    AdvogadoDetalheResponseDTO atualizarBiografia(AtualizarBiografiaAdvogadoInputDTO input);

    AdvogadoDetalheResponseDTO atualizarDisponibilidade(AtualizarDisponibilidadeAdvogadoInputDTO input);

    AdvogadoDetalheResponseDTO atualizarDocumentacao(AtualizarDocumentacaoAdvogadoInputDTO input);

    AdvogadoDetalheResponseDTO atualizarGraduacao(AtualizarGraduacaoAdvogadoInputDTO input);

    AdvogadoPerfilPublicoResponseDTO obterPerfilPublico(UUID advogadoId);

    record AvaliacoesPaginadas(AvaliacaoListagemResponseDTO data, PaginationInfo pagination) {}

    AvaliacoesPaginadas listarAvaliacoes(UUID advogadoId, int limit, int offset);

    AvaliacaoItemResponseDTO criarAvaliacao(UUID advogadoId, CriarAvaliacaoInputDTO input);

    UUID excluirAvaliacao(UUID advogadoId, UUID avaliacaoId);
}
