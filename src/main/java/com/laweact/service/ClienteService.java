package com.laweact.service;

import com.laweact.dto.cliente.CadastrarClienteInputDTO;
import com.laweact.dto.cliente.CadastrarClienteResponseDTO;

public interface ClienteService {

    CadastrarClienteResponseDTO cadastrar(CadastrarClienteInputDTO input);
}
