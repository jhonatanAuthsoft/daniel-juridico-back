package com.laweact.service.imp;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.laweact.dto.catalogo.EspecialidadeCatalogoItemDTO;
import com.laweact.dto.catalogo.SubespecialidadeCatalogoItemDTO;
import com.laweact.model.entity.EspecialidadeEntity;
import com.laweact.repository.EspecialidadeRepository;
import com.laweact.repository.SubespecialidadeRepository;
import com.laweact.service.CatalogoService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CatalogoServiceImp implements CatalogoService {

    private final EspecialidadeRepository especialidadeRepository;
    private final SubespecialidadeRepository subespecialidadeRepository;

    @Override
    @Transactional(readOnly = true)
    public List<EspecialidadeCatalogoItemDTO> listarEspecialidades() {
        List<EspecialidadeEntity> especialidades = especialidadeRepository.findAllByOrderByNomeAsc();
        return especialidades.stream()
                .map(esp -> EspecialidadeCatalogoItemDTO.builder()
                        .codigo(esp.getCodigo())
                        .nome(esp.getNome())
                        .subespecialidades(
                                subespecialidadeRepository.findByEspecialidadeIdOrderByNomeAsc(esp.getId())
                                        .stream()
                                        .map(sub -> SubespecialidadeCatalogoItemDTO.builder()
                                                .codigo(sub.getCodigo())
                                                .nome(sub.getNome())
                                                .build())
                                        .toList()
                        )
                        .build())
                .toList();
    }
}
