package com.laweact.service.imp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.laweact.config.exception.CustomError;
import com.laweact.dto.conexao.ConexaoListagemResponseDTO;
import com.laweact.dto.conexao.ConexaoResponseDTO;
import com.laweact.dto.conexao.CriarConexaoInputDTO;
import com.laweact.dto.shared.PaginationInfo;
import com.laweact.model.entity.AdvogadoEntity;
import com.laweact.model.entity.AvaliacaoAdvogadoEntity;
import com.laweact.model.entity.ClienteEntity;
import com.laweact.model.entity.ConexaoEntity;
import com.laweact.model.entity.EnderecoEntity;
import com.laweact.model.entity.SolicitacaoEntity;
import com.laweact.model.entity.UsuarioEntity;
import com.laweact.model.enums.DisponibilidadeAdvogadoEnum;
import com.laweact.model.enums.PerfilUsuarioEnum;
import com.laweact.model.enums.StatusConexaoEnum;
import com.laweact.model.enums.StatusSolicitacaoEnum;
import com.laweact.model.enums.TipoNotificacaoEnum;
import com.laweact.model.enums.UrgenciaSolicitacaoEnum;
import com.laweact.repository.AdvogadoRepository;
import com.laweact.repository.AvaliacaoAdvogadoRepository;
import com.laweact.repository.ClienteRepository;
import com.laweact.repository.ConexaoRepository;
import com.laweact.repository.EnderecoRepository;
import com.laweact.repository.SolicitacaoMatchRepository;
import com.laweact.repository.SolicitacaoRepository;
import com.laweact.repository.UsuarioRepository;
import com.laweact.service.ConexaoService;
import com.laweact.service.NotificacaoService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConexaoServiceImp implements ConexaoService {

    private static final Set<StatusSolicitacaoEnum> STATUS_SOLICITACAO_BLOQUEADOS =
            EnumSet.of(StatusSolicitacaoEnum.CANCELADA);

    private final ConexaoRepository conexaoRepository;
    private final SolicitacaoRepository solicitacaoRepository;
    private final SolicitacaoMatchRepository solicitacaoMatchRepository;
    private final ClienteRepository clienteRepository;
    private final AdvogadoRepository advogadoRepository;
    private final UsuarioRepository usuarioRepository;
    private final EnderecoRepository enderecoRepository;
    private final AvaliacaoAdvogadoRepository avaliacaoAdvogadoRepository;
    private final NotificacaoService notificacaoService;

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

        if (advogado.getDisponibilidade() != DisponibilidadeAdvogadoEnum.DISPONIVEL) {
            throw new CustomError(
                    "Este advogado está indisponível para novas conexões no momento",
                    HttpStatus.CONFLICT,
                    "LAWYER_UNAVAILABLE"
            );
        }

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

        ConexaoEntity detalhada = carregarDetalhada(salva.getId());
        ConexaoResponseDTO response = toResponse(detalhada);
        notificarSolicitada(detalhada);
        return response;
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

        ConexaoResponseDTO response = toResponse(conexao);
        notificarAceita(conexao);
        return response;
    }

    @Override
    @Transactional
    public ConexaoResponseDTO recusarDoAdvogadoAutenticado(UUID conexaoId) {
        return toResponse(decidirDoAdvogado(conexaoId, StatusConexaoEnum.RECUSADA));
    }

    @Override
    @Transactional
    public ConexaoResponseDTO marcarVisualizadaDoAdvogadoAutenticado(UUID conexaoId) {
        UsuarioEntity usuario = obterUsuarioAutenticado();
        if (usuario.getPerfil() != PerfilUsuarioEnum.ADVOGADO) {
            throw new CustomError("Apenas advogados podem visualizar conexão", HttpStatus.FORBIDDEN, "FORBIDDEN");
        }

        ConexaoEntity conexao = carregarDetalhada(conexaoId);
        if (!conexao.getAdvogado().getUsuarioId().equals(usuario.getId())) {
            throw new CustomError("Conexão de outro advogado", HttpStatus.FORBIDDEN, "FORBIDDEN");
        }

        if (conexao.getVisualizadaEm() == null) {
            conexao.setVisualizadaEm(LocalDateTime.now());
            conexaoRepository.save(conexao);
        }

        return toResponse(conexao);
    }

    @Override
    @Transactional(readOnly = true)
    public ListagemPaginada listarDoUsuarioAutenticado(
            int limit,
            int offset,
            List<StatusConexaoEnum> status,
            UrgenciaSolicitacaoEnum urgencia,
            String busca
    ) {
        UsuarioEntity usuario = obterUsuarioAutenticado();
        String buscaNormalizada = (busca == null || busca.isBlank()) ? "" : busca.trim();
        int offsetNormalizado = limit > 0 ? (Math.max(offset, 0) / limit) * limit : 0;
        Pageable pageable = limit > 0
                ? PageRequest.of(offsetNormalizado / limit, limit)
                : Pageable.unpaged();
        List<StatusConexaoEnum> statuses = statusParaFiltro(status);

        Page<ConexaoEntity> page = switch (usuario.getPerfil()) {
            case CLIENTE -> conexaoRepository.findForCliente(
                    usuario.getId(), statuses, urgencia, buscaNormalizada, pageable);
            case ADVOGADO -> conexaoRepository.findForAdvogado(
                    usuario.getId(), statuses, urgencia, buscaNormalizada, pageable);
        };

        ConexaoListagemResponseDTO data = ConexaoListagemResponseDTO.builder()
                .items(page.getContent().stream().map(this::toResponse).toList())
                .contagemPorUrgencia(carregarContagemPorUrgencia(usuario, statuses))
                .contagemPorStatus(carregarContagemPorStatus(usuario))
                .build();

        return new ListagemPaginada(
                data,
                PaginationInfo.of(limit, offsetNormalizado, page.getTotalElements())
        );
    }

    private List<StatusConexaoEnum> statusParaFiltro(List<StatusConexaoEnum> status) {
        if (status == null || status.isEmpty()) {
            return List.of(StatusConexaoEnum.values());
        }
        return List.copyOf(status);
    }

    private Map<UrgenciaSolicitacaoEnum, Long> carregarContagemPorUrgencia(
            UsuarioEntity usuario,
            List<StatusConexaoEnum> statuses
    ) {
        List<Object[]> linhas = switch (usuario.getPerfil()) {
            case CLIENTE -> conexaoRepository.countGroupedByUrgenciaForCliente(usuario.getId(), statuses);
            case ADVOGADO -> conexaoRepository.countGroupedByUrgenciaForAdvogado(usuario.getId(), statuses);
        };

        Map<UrgenciaSolicitacaoEnum, Long> contagem = ConexaoListagemResponseDTO.contagemVazia();
        for (Object[] linha : linhas) {
            contagem.put((UrgenciaSolicitacaoEnum) linha[0], (Long) linha[1]);
        }
        return contagem;
    }

    private Map<StatusConexaoEnum, Long> carregarContagemPorStatus(UsuarioEntity usuario) {
        List<Object[]> linhas = switch (usuario.getPerfil()) {
            case CLIENTE -> conexaoRepository.countGroupedByStatusForCliente(usuario.getId());
            case ADVOGADO -> conexaoRepository.countGroupedByStatusForAdvogado(usuario.getId());
        };

        Map<StatusConexaoEnum, Long> contagem = ConexaoListagemResponseDTO.contagemStatusVazia();
        for (Object[] linha : linhas) {
            contagem.put((StatusConexaoEnum) linha[0], (Long) linha[1]);
        }
        return contagem;
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

    private void notificarSolicitada(ConexaoEntity conexao) {
        String tituloSolicitacao = conexao.getSolicitacao().getTitulo();
        try {
            notificacaoService.criarETentarEnviar(
                    conexao.getAdvogado().getUsuarioId(),
                    conexao.getCliente().getUsuarioId(),
                    TipoNotificacaoEnum.CONEXAO_SOLICITADA,
                    conexao.getId(),
                    "Nova solicitação de conexão",
                    conexao.getCliente().getNomeCompleto()
                            + " solicitou conexão sobre \""
                            + tituloSolicitacao
                            + "\""
            );
        } catch (Exception ex) {
            log.error(
                    "Falha ao criar notificação CONEXAO_SOLICITADA para conexão {}",
                    conexao.getId(),
                    ex
            );
        }
    }

    private void notificarAceita(ConexaoEntity conexao) {
        String tituloSolicitacao = conexao.getSolicitacao().getTitulo();
        try {
            notificacaoService.criarETentarEnviar(
                    conexao.getCliente().getUsuarioId(),
                    conexao.getAdvogado().getUsuarioId(),
                    TipoNotificacaoEnum.CONEXAO_ACEITA,
                    conexao.getId(),
                    "Conexão aceita",
                    conexao.getAdvogado().getNomeCompleto()
                            + " aceitou sua solicitação \""
                            + tituloSolicitacao
                            + "\""
            );
        } catch (Exception ex) {
            log.error(
                    "Falha ao criar notificação CONEXAO_ACEITA para conexão {}",
                    conexao.getId(),
                    ex
            );
        }
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
        SolicitacaoEntity solicitacao = entity.getSolicitacao();
        ClienteEntity cliente = entity.getCliente();
        UsuarioEntity usuarioAdvogado = entity.getAdvogado().getUsuario();
        UsuarioEntity usuarioCliente = cliente.getUsuario();
        EnderecoEntity enderecoCliente = enderecoRepository
                .findByUsuario_Id(cliente.getUsuarioId())
                .orElse(null);

        AvaliacaoAdvogadoEntity avaliacaoCliente = avaliacaoAdvogadoRepository
                .findByConexao_Id(entity.getId())
                .orElse(null);
        BigDecimal avaliacaoNota = avaliacaoCliente != null ? avaliacaoCliente.getNota() : null;
        String avaliacaoComentario = avaliacaoCliente != null ? avaliacaoCliente.getComentario() : null;

        return ConexaoResponseDTO.builder()
                .id(entity.getId())
                .solicitacaoId(solicitacao.getId())
                .clienteId(cliente.getUsuarioId())
                .advogadoId(entity.getAdvogado().getUsuarioId())
                .status(entity.getStatus())
                .criadoEm(entity.getCreatedAt())
                .decididoEm(entity.getDecididoEm())
                .canceladoEm(entity.getCanceladoEm())
                .visualizadaEm(entity.getVisualizadaEm())
                .telefone(aceita ? usuarioAdvogado.getTelefone() : null)
                .email(aceita ? usuarioAdvogado.getEmail() : null)
                .nomeAdvogado(entity.getAdvogado().getNomeCompleto())
                .nomeCliente(cliente.getNomeCompleto())
                .tituloSolicitacao(solicitacao.getTitulo())
                .descricaoSolicitacao(solicitacao.getDescricao())
                .urgencia(solicitacao.getUrgencia())
                .modalidade(solicitacao.getModalidade())
                .especialidadeCodigo(solicitacao.getEspecialidadeCodigo())
                .subespecialidadeCodigo(solicitacao.getSubespecialidadeCodigo())
                .experienciaMinimaMeses(solicitacao.getExperienciaMinimaMeses())
                .uf(solicitacao.getUf())
                .cidade(solicitacao.getCidade())
                .formaCobranca(solicitacao.getFormaCobranca())
                .clienteProfissao(cliente.getProfissao())
                .clientePronomes(cliente.getPronomes() != null ? cliente.getPronomes().name() : null)
                .clienteEstadoCivil(cliente.getEstadoCivil())
                .clienteFaixaRenda(cliente.getFaixaRenda())
                .clienteFotoUrl(cliente.getFotoUrl())
                .clienteCidade(enderecoCliente != null ? enderecoCliente.getCidade() : null)
                .clienteUf(enderecoCliente != null ? enderecoCliente.getEstado() : null)
                .clienteTelefone(aceita && usuarioCliente != null ? usuarioCliente.getTelefone() : null)
                .clienteEmail(aceita && usuarioCliente != null ? usuarioCliente.getEmail() : null)
                .avaliacaoClienteNota(avaliacaoNota)
                .avaliacaoClienteComentario(avaliacaoComentario)
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
