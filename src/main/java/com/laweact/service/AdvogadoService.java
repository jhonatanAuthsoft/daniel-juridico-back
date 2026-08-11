package com.laweact.service;

import com.laweact.dto.advogado.CadastrarAdvogadoInputDTO;
import com.laweact.dto.advogado.CadastrarAdvogadoResponseDTO;

public interface AdvogadoService {

    CadastrarAdvogadoResponseDTO cadastrar(CadastrarAdvogadoInputDTO input);
}
