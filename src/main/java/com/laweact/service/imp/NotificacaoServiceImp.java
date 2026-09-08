package com.laweact.service.imp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.laweact.config.ExpoPushProperties;
import com.laweact.config.exception.CustomError;
import com.laweact.dto.notificacao.NaoLidasExisteResponseDTO;
import com.laweact.dto.notificacao.NotificacaoResponseDTO;
import com.laweact.model.entity.DispositivoPushEntity;
import com.laweact.model.entity.NotificacaoEntity;
import com.laweact.model.entity.SolicitacaoEntity;
import com.laweact.model.entity.UsuarioEntity;
import com.laweact.model.enums.PerfilUsuarioEnum;
import com.laweact.model.enums.ReferenciaNotificacaoEnum;
import com.laweact.model.enums.StatusEnvioNotificacaoEnum;
import com.laweact.model.enums.TipoNotificacaoEnum;
import com.laweact.model.enums.UrgenciaSolicitacaoEnum;
import com.laweact.repository.ConexaoRepository;
import com.laweact.repository.DispositivoPushRepository;
import com.laweact.repository.NotificacaoRepository;
import com.laweact.repository.SolicitacaoRepository;
import com.laweact.repository.UsuarioRepository;
import com.laweact.service.ExpoPushClient;
import com.laweact.service.ExpoPushClient.ExpoPushSendResult;
import com.laweact.service.NotificacaoService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacaoServiceImp implements NotificacaoService {

    private final NotificacaoRepository notificacaoRepository;
    private final DispositivoPushRepository dispositivoPushRepository;
    private final UsuarioRepository usuarioRepository;
    private final ExpoPushClient expoPushClient;
    private final ExpoPushProperties expoPushProperties;
    private final SolicitacaoRepository solicitacaoRepository;
    private final ConexaoRepository conexaoRepository;

    @Override
    @Transactional(readOnly = true)
    public List<NotificacaoResponseDTO> listarDoUsuarioAutenticado(int limit, int offset) {
        UsuarioEntity usuario = obterUsuarioAutenticado();
        int pageSize = limit > 0 ? limit : 20;
        int pageIndex = Math.max(offset, 0) / pageSize;
        PageRequest pageable = PageRequest.of(pageIndex, pageSize);

        return notificacaoRepository
                .findByDestinatario_IdOrderByCreatedAtDesc(usuario.getId(), pageable)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public NaoLidasExisteResponseDTO existeNaoLida() {
        UsuarioEntity usuario = obterUsuarioAutenticado();
        boolean existe = notificacaoRepository.existsByDestinatario_IdAndLidaEmIsNull(usuario.getId());
        return NaoLidasExisteResponseDTO.builder().existe(existe).build();
    }

    @Override
    @Transactional
    public NotificacaoResponseDTO ler(UUID id) {
        UsuarioEntity usuario = obterUsuarioAutenticado();
        NotificacaoEntity notificacao = notificacaoRepository
                .findByIdAndDestinatario_Id(id, usuario.getId())
                .orElseThrow(() -> new CustomError("Notificação não encontrada", HttpStatus.NOT_FOUND));

        if (notificacao.getLidaEm() == null) {
            notificacao.setLidaEm(LocalDateTime.now());
            notificacao = notificacaoRepository.save(notificacao);
        }
        return toResponse(notificacao);
    }

    @Override
    @Transactional
    public void lerTodas() {
        UsuarioEntity usuario = obterUsuarioAutenticado();
        notificacaoRepository.marcarTodasLidas(usuario.getId(), LocalDateTime.now());
    }

    @Override
    @Transactional
    public void lerPorSolicitacao(UUID solicitacaoId) {
        UsuarioEntity usuario = obterUsuarioAutenticado();
        SolicitacaoEntity solicitacao = solicitacaoRepository.findById(solicitacaoId)
                .orElseThrow(() -> new CustomError("Solicitação não encontrada", HttpStatus.NOT_FOUND));

        List<UUID> conexaoIds = conexaoIdsAutorizadas(usuario, solicitacao);
        if (conexaoIds.isEmpty()) {
            return;
        }
        notificacaoRepository.marcarLidasPorReferencias(usuario.getId(), conexaoIds, LocalDateTime.now());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public NotificacaoEntity criarETentarEnviar(
            UUID destinatarioId,
            UUID remetenteId,
            TipoNotificacaoEnum tipo,
            UUID conexaoId,
            String titulo,
            String texto,
            UrgenciaSolicitacaoEnum urgencia
    ) {
        UsuarioEntity destinatario = usuarioRepository.findById(destinatarioId)
                .orElseThrow(() -> new CustomError("Destinatário não encontrado", HttpStatus.NOT_FOUND));
        UsuarioEntity remetente = usuarioRepository.findById(remetenteId)
                .orElseThrow(() -> new CustomError("Remetente não encontrado", HttpStatus.NOT_FOUND));

        NotificacaoEntity notificacao = notificacaoRepository.save(NotificacaoEntity.builder()
                .destinatario(destinatario)
                .remetente(remetente)
                .titulo(titulo)
                .texto(texto)
                .tipo(tipo)
                .referenciaTipo(ReferenciaNotificacaoEnum.CONEXAO)
                .referenciaId(conexaoId)
                .statusEnvio(StatusEnvioNotificacaoEnum.PENDENTE)
                .build());
        log.info(
                "Notificação {} criada tipo={} destinatario={} conexao={}",
                notificacao.getId(),
                tipo,
                destinatarioId,
                conexaoId
        );

        return tentarEnviarPush(notificacao, urgencia);
    }

    @Override
    @Transactional
    public NotificacaoEntity reinsistirEnvio(
            NotificacaoEntity notificacao,
            String titulo,
            String texto,
            UrgenciaSolicitacaoEnum urgencia
    ) {
        notificacao.setLidaEm(null);
        notificacao.setTitulo(titulo);
        notificacao.setTexto(texto);
        notificacao.setStatusEnvio(StatusEnvioNotificacaoEnum.PENDENTE);
        notificacao.setErroEnvio(null);
        notificacao = notificacaoRepository.save(notificacao);
        return tentarEnviarPush(notificacao, urgencia);
    }

    private NotificacaoEntity tentarEnviarPush(
            NotificacaoEntity notificacao,
            UrgenciaSolicitacaoEnum urgencia
    ) {
        UsuarioEntity destinatario = notificacao.getDestinatario();
        UUID destinatarioId = destinatario.getId();
        String titulo = notificacao.getTitulo();
        String texto = notificacao.getTexto();
        TipoNotificacaoEnum tipo = notificacao.getTipo();
        UUID conexaoId = notificacao.getReferenciaId();

        if (!Boolean.TRUE.equals(destinatario.getNotificacoesPushHabilitadas())) {
            return marcarSkipped(notificacao);
        }

        if (!expoPushProperties.isEnabled()) {
            return marcarSkipped(notificacao);
        }

        List<DispositivoPushEntity> dispositivos =
                dispositivoPushRepository.findByUsuario_IdAndAtivoTrue(destinatarioId);
        if (dispositivos.isEmpty()) {
            return marcarSkipped(notificacao);
        }

        boolean algumSucesso = false;
        List<String> erros = new ArrayList<>();

        for (DispositivoPushEntity dispositivo : dispositivos) {
            ExpoPushSendResult resultado = expoPushClient.enviar(
                    dispositivo.getExpoPushToken(),
                    titulo,
                    texto,
                    tipo,
                    conexaoId,
                    urgencia
            );

            switch (resultado) {
                case ExpoPushSendResult.Success ignored -> algumSucesso = true;
                case ExpoPushSendResult.DeviceNotRegistered ignored -> {
                    dispositivo.setAtivo(false);
                    dispositivoPushRepository.save(dispositivo);
                    log.info("Token Expo desativado (DeviceNotRegistered): {}", dispositivo.getId());
                }
                case ExpoPushSendResult.Error error -> erros.add(error.message());
            }
        }

        if (algumSucesso) {
            notificacao.setStatusEnvio(StatusEnvioNotificacaoEnum.ENVIADA);
            notificacao.setEnviadoEm(LocalDateTime.now());
            notificacao.setErroEnvio(erros.isEmpty() ? null : truncar(String.join("; ", erros)));
            return notificacaoRepository.save(notificacao);
        }

        notificacao.setStatusEnvio(StatusEnvioNotificacaoEnum.ERROR);
        notificacao.setErroEnvio(
                erros.isEmpty()
                        ? "Nenhum dispositivo válido para envio"
                        : truncar(String.join("; ", erros))
        );
        return notificacaoRepository.save(notificacao);
    }

    private NotificacaoEntity marcarSkipped(NotificacaoEntity notificacao) {
        notificacao.setStatusEnvio(StatusEnvioNotificacaoEnum.SKIPPED);
        return notificacaoRepository.save(notificacao);
    }

    private NotificacaoResponseDTO toResponse(NotificacaoEntity entity) {
        return NotificacaoResponseDTO.builder()
                .id(entity.getId())
                .titulo(entity.getTitulo())
                .texto(entity.getTexto())
                .tipo(entity.getTipo())
                .referenciaTipo(entity.getReferenciaTipo())
                .referenciaId(entity.getReferenciaId())
                .remetenteId(entity.getRemetente().getId())
                .criadoEm(entity.getCreatedAt())
                .lidaEm(entity.getLidaEm())
                .statusEnvio(entity.getStatusEnvio())
                .build();
    }

    private List<UUID> conexaoIdsAutorizadas(UsuarioEntity usuario, SolicitacaoEntity solicitacao) {
        UUID solicitacaoId = solicitacao.getId();
        if (usuario.getPerfil() == PerfilUsuarioEnum.CLIENTE) {
            if (!solicitacao.getCliente().getUsuarioId().equals(usuario.getId())) {
                throw new CustomError("Solicitação de outro cliente", HttpStatus.FORBIDDEN, "FORBIDDEN");
            }
            return conexaoRepository.findIdsBySolicitacaoId(solicitacaoId);
        }
        if (usuario.getPerfil() == PerfilUsuarioEnum.ADVOGADO) {
            return conexaoRepository
                    .findBySolicitacao_IdAndAdvogado_UsuarioId(solicitacaoId, usuario.getId())
                    .map(conexao -> List.of(conexao.getId()))
                    .orElseThrow(() -> new CustomError(
                            "Solicitação de outro advogado",
                            HttpStatus.FORBIDDEN,
                            "FORBIDDEN"
                    ));
        }
        throw new CustomError("Perfil não autorizado", HttpStatus.FORBIDDEN, "FORBIDDEN");
    }

    private UsuarioEntity obterUsuarioAutenticado() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof UserDetails userDetails)) {
            throw new CustomError("Usuário não autenticado", HttpStatus.UNAUTHORIZED);
        }
        return usuarioRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new CustomError("Usuário não encontrado", HttpStatus.NOT_FOUND));
    }

    private static String truncar(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}
