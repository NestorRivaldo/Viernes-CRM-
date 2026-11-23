package com.viernes.clinica.adapters.in.web;

import com.viernes.clinica.application.dto.EspecialidadResponse;
import com.viernes.clinica.application.service.ConsultorioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Especialidades", description = "Catálogo de Especialidades - LP2 Examen Variante 7")
@RestController
@RequestMapping({"/api/clinica/especialidades", "/api/v1/clinica/especialidades"})
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class EspecialidadController {

    private final ConsultorioService service;

    @Operation(summary = "Listar todas las especialidades precargadas")
    @GetMapping
    public ResponseEntity<List<EspecialidadResponse>> listar() {
        return ResponseEntity.ok(service.listarEspecialidades());
    }
}
