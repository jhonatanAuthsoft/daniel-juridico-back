package com.laweact.service;

import java.util.UUID;

public interface AuditoriaService {

    void registrar(String evento, UUID usuarioId, Object input, Object response, Object detalhes);
}
