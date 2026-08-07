package com.laweact.service.imp;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.laweact.config.exception.CustomError;
import com.laweact.dto.conexao.ConexaoResponseDTO;
import com.laweact.dto.conexao.CriarConexaoInputDTO;
import com.laweact.model.entity.AdvogadoEntity;
import com.laweact.model.entity.ClienteEntity;
import com.laweact.model.entity.ConexaoEntity;
import com.laweact.model.entity.SolicitacaoEntity;
import com.laweact.model.entity.UsuarioEntity;
import com.laweact.model.enums.PerfilUsuarioEnum;
import com.laweact.model.enums.StatusConexaoEnum;
import com.laweact.model.enums.StatusSolicitacaoEnum;
import com.laweact.repository.AdvogadoRepository;
import com.laweact.repository.ClienteRepository;
import com.laweact.repository.ConexaoRepository;
import com.laweact.repository.SolicitacaoMatchRepository;
import com.laweact.repository.SolicitacaoRepository;
import com.laweact.repository.UsuarioRepository;
import com.laweact.service.ConexaoService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConexaoServiceImp implements ConexaoService {

    private static final Set<StatusSolicitacaoEnum> STATUS_SOLICITACAO_BLOQUEADOS =
            EnumSet.of(StatusSolicitacaoEnum.CANCELADA, StatusSolicitacaoEnum.ENCERRADA);

    private final ConexaoRepository conexaoRepository;
    private final SolicitacaoRepository solicitacaoRepository;
    private final SolicitacaoMatchRepository solicitacaoMatchRepository;
    private final ClienteRepository clienteRepository;
    private final AdvogadoRepository advogadoRepository;
    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional
    public ConexaoResponseDTO criar(CriarConexaoInputDTO input) {
        UsuarioEntity usuario = obterUsuarioAutenticado();
        if (usuario.getPerfil() != PerfilUsuarioEnum.CLIENTE) {
            throw new CustomError("Apenas clientes podem solicitar conexão", HttpStatus.FORBIDDEN, "FORBIDDEN");
        }

        ClienteEntity cliente = clienteRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new CustomError("Perfil de cliente não encontrado", HttpStatus.NOT_FOUND));

        SolicitacaoEntity solicitacao = solicitacaoRepository.findById(input.solicitacaoId())
                .orElseThrow(() -> new CustomError("Solicitação não encontrada", HttpStatus.NOT_FOUND));

        if (!solicitacao.getCliente().getUsuarioId().equals(cliente.getUsuarioId())) {
            throw new CustomError("Solicitação de outro cliente", HttpStatus.FORBIDDEN, "FORBIDDEN");
        }
        if (STATUS_SOLICITACAO_BLOQUEADOS.contains(solicitacao.getStatus())) {
            throw new CustomError(
                    "Solicitação não permite novas conexões no status atual",
                    HttpStatus.CONFLICT,
                    "INVALID_STATUS"
            );
        }

        AdvogadoEntity advogado = advogadoRepository.findByUsuarioId(input.advogadoId())
                .orElseThrow(() -> new CustomError("Advogado não encontrado", HttpStatus.NOT_FOUND));

        if (!solicitacaoMatchRepository.existsBySolicitacao_IdAndAdvogado_UsuarioId(
                solicitacao.getId(),
                advogado.getUsuarioId()
        )) {
            throw new CustomError(
                    "Advogado não está no ranking de matches desta solicitação",
                    HttpStatus.BAD_REQUEST,
                    "NO_MATCH"
            );
        }

        var existente = conexaoRepository.findBySolicitacao_IdAndAdvogado_UsuarioId(
                solicitacao.getId(),
                advogado.getUsuarioId()
        );

        ConexaoEntity salva;
        if (existente.isPresent()) {
            ConexaoEntity conexao = existente.get();
            switch (conexao.getStatus()) {
                case CANCELADA -> {
                    conexao.setStatus(StatusConexaoEnum.PENDENTE);
                    conexao.setCanceladoEm(null);
                    conexao.setDecididoEm(null);
                    salva = conexaoRepository.save(conexao);
                }
                case RECUSADA, PENDENTE, ACEITA -> throw new CustomError(
                        "Já existe conexão neste status para o par informado",
                        HttpStatus.CONFLICT,
                        "INVALID_STATUS"
                );
                default -> throw new CustomError(
                        "Status de conexão inválido",
                        HttpStatus.CONFLICT,
                        "INVALID_STATUS"
                );
            }
        } else {
            salva = conexaoRepository.save(ConexaoEntity.builder()
                    .solicitacao(solicitacao)
                    .cliente(cliente)
                    .advogado(advogado)
                    .status(StatusConexaoEnum.PENDENTE)
                    .build());
        }

        log.info(
                "Conexão {} PENDENTE — solicitação {} cliente {} advogado {}",
                salva.getId(),
                solicitacao.getId(),
                cliente.getUsuarioId(),
                advogado.getUsuarioId()
        );
        return toResponse(carregarDetalhada(salva.getId()));
    }

    @Override
    @Transactional
    public ConexaoResponseDTO cancelarDoClienteAutenticado(UUID conexaoId) {
        UsuarioEntity usuario = obterUsuarioAutenticado();
        if (usuario.getPerfil() != PerfilUsuarioEnum.CLIENTE) {
            throw new CustomError("Apenas clientes podem cancelar conexão", HttpStatus.FORBIDDEN, "FORBIDDEN");
        }

        ConexaoEntity conexao = carregarDetalhada(conexaoId);
        if (!conexao.getCliente().getUsuarioId().equals(usuario.getId())) {
            throw new CustomError("Conexão de outro cliente", HttpStatus.FORBIDDEN, "FORBIDDEN");
        }
        if (conexao.getStatus() != StatusConexaoEnum.PENDENTE) {
            throw new CustomError(
                    "Somente conexões pendentes podem ser canceladas",
                    HttpStatus.CONFLICT,
                    "INVALID_STATUS"
            );
        }

        conexao.setStatus(StatusConexaoEnum.CANCELADA);
        conexao.setCanceladoEm(LocalDateTime.now());
        conexaoRepository.save(conexao);
        return toResponse(conexao);
    }

    @Override
    @Transactional
    public ConexaoResponseDTO aceitarDoAdvogadoAutenticado(UUID conexaoId) {
        ConexaoEntity conexao = decidirDoAdvogado(conexaoId, StatusConexaoEnum.ACEITA);

        SolicitacaoEntity solicitacao = conexao.getSolicitacao();
        if (solicitacao.getStatus() != StatusSolicitacaoEnum.MATCH_REALIZADO
                && !STATUS_SOLICITACAO_BLOQUEADOS.contains(solicitacao.getStatus())) {
            solicitacao.setStatus(StatusSolicitacaoEnum.MATCH_REALIZADO);
            solicitacaoRepository.save(solicitacao);
        }

        return toResponse(conexao);
    }

    @Override
    @Transactional
    public ConexaoResponseDTO recusarDoAdvogadoAutenticado(UUID conexaoId) {
        return toResponse(decidirDoAdvogado(conexaoId, StatusConexaoEnum.RECUSADA));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConexaoResponseDTO> listarDoUsuarioAutenticado(StatusConexaoEnum status) {
        UsuarioEntity usuario = obterUsuarioAutenticado();
        List<ConexaoEntity> lista = switch (usuario.getPerfil()) {
            case CLIENTE -> conexaoRepository.findByCliente(usuario.getId(), status);
            case ADVOGADO -> conexaoRepository.findByAdvogado(usuario.getId(), status);
        };
        return lista.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConexaoResponseDTO> listarPorSolicitacaoDoCliente(UUID solicitacaoId) {
        UsuarioEntity usuario = obterUsuarioAutenticado();
        if (usuario.getPerfil() != PerfilUsuarioEnum.CLIENTE) {
            throw new CustomError("Apenas clientes podem listar conexões da solicitação", HttpStatus.FORBIDDEN, "FORBIDDEN");
        }

        SolicitacaoEntity solicitacao = solicitacaoRepository.findById(solicitacaoId)
                .orElseThrow(() -> new CustomError("Solicitação não encontrada", HttpStatus.NOT_FOUND));
        if (!solicitacao.getCliente().getUsuarioId().equals(usuario.getId())) {
            throw new CustomError("Solicitação de outro cliente", HttpStatus.FORBIDDEN, "FORBIDDEN");
        }

        return conexaoRepository.findBySolicitacaoId(solicitacaoId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ConexaoResponseDTO buscarPorAdvogadoESolicitacao(UUID advogadoId, UUID solicitacaoId) {
        UsuarioEntity usuario = obterUsuarioAutenticado();
        if (usuario.getPerfil() != PerfilUsuarioEnum.CLIENTE) {
            throw new CustomError("Apenas clientes podem consultar status de conexão", HttpStatus.FORBIDDEN, "FORBIDDEN");
        }

        SolicitacaoEntity solicitacao = solicitacaoRepository.findById(solicitacaoId)
                .orElseThrow(() -> new CustomError("Solicitação não encontrada", HttpStatus.NOT_FOUND));
        if (!solicitacao.getCliente().getUsuarioId().equals(usuario.getId())) {
            throw new CustomError("Solicitação de outro cliente", HttpStatus.FORBIDDEN, "FORBIDDEN");
        }

        ConexaoEntity conexao = conexaoRepository
                .findBySolicitacao_IdAndAdvogado_UsuarioId(solicitacaoId, advogadoId)
                .orElseThrow(() -> new CustomError("Conexão não encontrada", HttpStatus.NOT_FOUND));

        return toResponse(carregarDetalhada(conexao.getId()));
    }

    private ConexaoEntity decidirDoAdvogado(UUID conexaoId, StatusConexaoEnum novoStatus) {
        UsuarioEntity usuario = obterUsuarioAutenticado();
        if (usuario.getPerfil() != PerfilUsuarioEnum.ADVOGADO) {
            throw new CustomError("Apenas advogados podem decidir conexão", HttpStatus.FORBIDDEN, "FORBIDDEN");
        }

        ConexaoEntity conexao = carregarDetalhada(conexaoId);
        if (!conexao.getAdvogado().getUsuarioId().equals(usuario.getId())) {
            throw new CustomError("Conexão de outro advogado", HttpStatus.FORBIDDEN, "FORBIDDEN");
        }
        if (conexao.getStatus() != StatusConexaoEnum.PENDENTE) {
            throw new CustomError(
                    "Somente conexões pendentes podem ser decididas",
                    HttpStatus.CONFLICT,
                    "INVALID_STATUS"
            );
        }

        conexao.setStatus(novoStatus);
        conexao.setDecididoEm(LocalDateTime.now());
        return conexaoRepository.save(conexao);
    }

    private ConexaoEntity carregarDetalhada(UUID id) {
        return conexaoRepository.findDetailedById(id)
                .orElseThrow(() -> new CustomError("Conexão não encontrada", HttpStatus.NOT_FOUND));
    }

    private ConexaoResponseDTO toResponse(ConexaoEntity entity) {
        boolean aceita = entity.getStatus() == StatusConexaoEnum.ACEITA;
        UsuarioEntity usuarioAdvogado = entity.getAdvogado().getUsuario();

        return ConexaoResponseDTO.builder()
                .id(entity.getId())
                .solicitacaoId(entity.getSolicitacao().getId())
                .clienteId(entity.getCliente().getUsuarioId())
                .advogadoId(entity.getAdvogado().getUsuarioId())
                .status(entity.getStatus())
                .criadoEm(entity.getCreatedAt())
                .decididoEm(entity.getDecididoEm())
                .canceladoEm(entity.getCanceladoEm())
                .telefone(aceita ? usuarioAdvogado.getTelefone() : null)
                .email(aceita ? usuarioAdvogado.getEmail() : null)
                .nomeAdvogado(entity.getAdvogado().getNomeCompleto())
                .nomeCliente(entity.getCliente().getNomeCompleto())
                .tituloSolicitacao(entity.getSolicitacao().getTitulo())
                .build();
    }

    private UsuarioEntity obterUsuarioAutenticado() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof UserDetails userDetails)) {
            throw new CustomError("Usuário não autenticado", HttpStatus.UNAUTHORIZED);
        }
        return usuarioRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new CustomError("Usuário não encontrado", HttpStatus.NOT_FOUND));
    }
}
