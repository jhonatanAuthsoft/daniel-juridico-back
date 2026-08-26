package com.laweact.service;

import com.laweact.dto.arquivo.ArquivoUrlLeituraInputDTO;
import com.laweact.dto.arquivo.ArquivoUrlLeituraResponseDTO;
import com.laweact.dto.arquivo.ArquivoUrlUploadInputDTO;
import com.laweact.dto.arquivo.ArquivoUrlUploadResponseDTO;
import com.laweact.model.enums.ArquivoFinalidade;

public interface ArquivoService {

    ArquivoUrlUploadResponseDTO criarUrlUpload(ArquivoUrlUploadInputDTO input);

    ArquivoUrlLeituraResponseDTO criarUrlLeitura(ArquivoUrlLeituraInputDTO input);

    void validarKeyParaFinalidade(String key, ArquivoFinalidade finalidade);
}
