package com.laweact.service;

import java.util.List;
import java.util.UUID;

import com.laweact.dto.notificacao.NaoLidasExisteResponseDTO;
import com.laweact.dto.notificacao.NotificacaoResponseDTO;
import com.laweact.model.entity.NotificacaoEntity;
import com.laweact.model.enums.TipoNotificacaoEnum;

public interface NotificacaoService {

    List<NotificacaoResponseDTO> listarDoUsuarioAutenticado(int limit, int offset);

    NaoLidasExisteResponseDTO existeNaoLida();

    NotificacaoResponseDTO ler(UUID id);

    void lerTodas();

    NotificacaoEntity criarETentarEnviar(
            UUID destinatarioId,
            UUID remetenteId,
            TipoNotificacaoEnum tipo,
            UUID conexaoId,
            String titulo,
            String texto
    );
}
