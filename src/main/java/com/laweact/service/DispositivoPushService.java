package com.laweact.service;

import com.laweact.dto.dispositivo.DispositivoPushResponseDTO;
import com.laweact.dto.dispositivo.RegistrarDispositivoPushInputDTO;
import com.laweact.dto.dispositivo.RemoverDispositivoPushInputDTO;

public interface DispositivoPushService {

    DispositivoPushResponseDTO registrar(RegistrarDispositivoPushInputDTO input);

    DispositivoPushResponseDTO remover(RemoverDispositivoPushInputDTO input);
}
