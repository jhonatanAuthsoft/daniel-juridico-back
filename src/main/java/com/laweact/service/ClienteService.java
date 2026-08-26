package com.laweact.service;

import com.laweact.dto.cliente.AtualizarDadosGeraisClienteInputDTO;
import com.laweact.dto.cliente.AtualizarEnderecoClienteInputDTO;
import com.laweact.dto.cliente.AtualizarPerfilPessoalClienteInputDTO;
import com.laweact.dto.cliente.CadastrarClienteInputDTO;
import com.laweact.dto.cliente.CadastrarClienteResponseDTO;
import com.laweact.dto.cliente.ClienteDetalheResponseDTO;

public interface ClienteService {

    CadastrarClienteResponseDTO cadastrar(CadastrarClienteInputDTO input);

    ClienteDetalheResponseDTO atualizarDadosGerais(AtualizarDadosGeraisClienteInputDTO input);

    ClienteDetalheResponseDTO atualizarEndereco(AtualizarEnderecoClienteInputDTO input);

    ClienteDetalheResponseDTO atualizarPerfilPessoal(AtualizarPerfilPessoalClienteInputDTO input);
}
