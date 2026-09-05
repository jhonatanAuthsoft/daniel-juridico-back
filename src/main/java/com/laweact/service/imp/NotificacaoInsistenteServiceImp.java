package com.laweact.service.imp;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.laweact.config.NotificacaoInsistenteProperties;
import com.laweact.dto.job.NotificacaoInsistenteJobResultDTO;
import com.laweact.model.entity.ConexaoEntity;
import com.laweact.model.entity.NotificacaoEntity;
import com.laweact.model.enums.StatusConexaoEnum;
import com.laweact.model.enums.TipoNotificacaoEnum;
import com.laweact.model.enums.UrgenciaSolicitacaoEnum;
import com.laweact.repository.ConexaoRepository;
import com.laweact.repository.NotificacaoRepository;
import com.laweact.service.NotificacaoInsistenteService;
import com.laweact.service.NotificacaoService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacaoInsistenteServiceImp implements NotificacaoInsistenteService {

    private static final Set<UrgenciaSolicitacaoEnum> URGENCIAS_INSISTENTES = EnumSet.of(
            UrgenciaSolicitacaoEnum.EMERGENCIA,
            UrgenciaSolicitacaoEnum.URGENTE
    );

    private final ConexaoRepository conexaoRepository;
    private final NotificacaoRepository notificacaoRepository;
    private final NotificacaoService notificacaoService;
    private final NotificacaoInsistenteProperties properties;

    @Override
    @Transactional
    public NotificacaoInsistenteJobResultDTO processar() {
        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime limite = agora.minus(properties.getInterval());

        List<ConexaoEntity> candidatas = conexaoRepository.findPendentesParaLembreteInsistente(
                StatusConexaoEnum.PENDENTE,
                URGENCIAS_INSISTENTES,
                limite
        );

        int reenviadas = 0;
        int ignoradas = 0;
        int erros = 0;

        for (ConexaoEntity conexao : candidatas) {
            try {
                boolean enviou = reinsistirConexao(conexao, agora);
                if (enviou) {
                    reenviadas++;
                } else {
                    ignoradas++;
                }
            } catch (Exception ex) {
                erros++;
                log.error(
                        "Falha ao processar lembrete insistente da conexão {}",
                        conexao.getId(),
                        ex
                );
            }
        }

        return NotificacaoInsistenteJobResultDTO.builder()
                .avaliadas(candidatas.size())
                .reenviadas(reenviadas)
                .ignoradas(ignoradas)
                .erros(erros)
                .build();
    }

    private boolean reinsistirConexao(ConexaoEntity conexao, LocalDateTime agora) {
        if (conexao.getVisualizadaEm() != null) {
            return false;
        }

        Optional<NotificacaoEntity> existente = notificacaoRepository
                .findFirstByReferenciaIdAndTipoOrderByCreatedAtAsc(
                        conexao.getId(),
                        TipoNotificacaoEnum.CONEXAO_SOLICITADA
                );

        if (existente.map(NotificacaoEntity::getLidaEm).isPresent()) {
            return false;
        }

        String nomeCliente = conexao.getCliente().getUsuario().getNomeCompleto();
        String tituloSolicitacao = conexao.getSolicitacao().getTitulo();
        UrgenciaSolicitacaoEnum urgencia = conexao.getSolicitacao().getUrgencia();
        String titulo = "Lembrete: solicitação de conexão";
        String texto = montarTextoLembrete(nomeCliente, tituloSolicitacao, urgencia);

        if (existente.isPresent()) {
            notificacaoService.reinsistirEnvio(existente.get(), titulo, texto);
        } else {
            notificacaoService.criarETentarEnviar(
                    conexao.getAdvogado().getUsuarioId(),
                    conexao.getCliente().getUsuarioId(),
                    TipoNotificacaoEnum.CONEXAO_SOLICITADA,
                    conexao.getId(),
                    titulo,
                    texto
            );
        }

        conexao.setUltimoLembreteInsistenteEm(agora);
        conexaoRepository.save(conexao);
        return true;
    }

    private static String montarTextoLembrete(
            String nomeCliente,
            String tituloSolicitacao,
            UrgenciaSolicitacaoEnum urgencia
    ) {
        String grau = urgencia == UrgenciaSolicitacaoEnum.EMERGENCIA ? "emergência" : "urgência";
        return nomeCliente
                + " ainda aguarda resposta ("
                + grau
                + ") sobre \""
                + tituloSolicitacao
                + "\"";
    }
}
