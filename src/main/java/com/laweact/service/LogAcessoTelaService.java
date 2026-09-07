package com.laweact.service;

import com.laweact.dto.usuario.LogAcessoTelaResponseDTO;
import com.laweact.dto.usuario.RegistrarAcessoTelaInputDTO;

public interface LogAcessoTelaService {

    LogAcessoTelaResponseDTO registrar(RegistrarAcessoTelaInputDTO input);
}
