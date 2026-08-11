package com.laweact.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.laweact.dto.catalogo.EspecialidadeCatalogoItemDTO;
import com.laweact.dto.shared.ApiResponse;
import com.laweact.service.CatalogoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/catalogos")
@Tag(name = "Catálogos", description = "Listagens de domínio para o front (fonte única de verdade)")
public class CatalogoController {

    private final CatalogoService catalogoService;

    @GetMapping("/especialidades")
    @Operation(
            summary = "Listar especialidades",
            description = "Retorna especialidades e subespecialidades do banco para o cadastro/busca do app"
    )
    public ResponseEntity<ApiResponse<List<EspecialidadeCatalogoItemDTO>>> listarEspecialidades() {
        List<EspecialidadeCatalogoItemDTO> data = catalogoService.listarEspecialidades();
        return ResponseEntity.ok(ApiResponse.success(data, "Consulta realizada com sucesso"));
    }
}
