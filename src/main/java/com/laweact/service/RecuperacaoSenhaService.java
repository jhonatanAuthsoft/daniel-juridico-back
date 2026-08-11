package com.laweact.service;

import com.laweact.dto.usuario.RedefinirSenhaInputDTO;
import com.laweact.dto.usuario.RedefinirSenhaResponseDTO;
import com.laweact.dto.usuario.SolicitarRecuperacaoSenhaInputDTO;
import com.laweact.dto.usuario.SolicitarRecuperacaoSenhaResponseDTO;
import com.laweact.dto.usuario.ValidarCodigoRecuperacaoInputDTO;
import com.laweact.dto.usuario.ValidarCodigoRecuperacaoResponseDTO;

public interface RecuperacaoSenhaService {

    SolicitarRecuperacaoSenhaResponseDTO solicitarCodigo(SolicitarRecuperacaoSenhaInputDTO input);

    ValidarCodigoRecuperacaoResponseDTO validarCodigo(ValidarCodigoRecuperacaoInputDTO input);

    RedefinirSenhaResponseDTO redefinirSenha(RedefinirSenhaInputDTO input);
}
