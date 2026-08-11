package com.laweact.service.imp;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laweact.model.entity.AuditoriaEventoEntity;
import com.laweact.repository.AuditoriaEventoRepository;
import com.laweact.service.AuditoriaService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@RequiredArgsConstructor
public class AuditoriaServiceImp implements AuditoriaService {

    private final AuditoriaEventoRepository auditoriaEventoRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void registrar(String evento, UUID usuarioId, Object input, Object response, Object detalhes) {
        try {
            AuditoriaEventoEntity entity = AuditoriaEventoEntity.builder()
                    .evento(evento)
                    .usuarioId(usuarioId)
                    .input(toJson(input))
                    .response(toJson(response))
                    .detalhes(toJson(detalhes))
                    .build();
            auditoriaEventoRepository.save(entity);
        } catch (Exception ex) {
            log.warn("Falha ao registrar auditoria evento={}: {}", evento, ex.getMessage());
        }
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String text) {
            return text;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return String.valueOf(value);
        }
    }
}
