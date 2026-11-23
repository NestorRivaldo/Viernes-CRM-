package com.viernes.clinica.adapters.in.web;

import com.viernes.clinica.application.dto.ClinicaRequest;
import com.viernes.clinica.application.dto.ClinicaResponse;
import com.viernes.clinica.application.service.ClinicaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Clínicas", description = "Gestión de Clínicas - LP2 Examen Variante 7")
@RestController
@RequestMapping({"/api/clinica/clinicas", "/api/v1/clinica/clinicas"})
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ClinicaController {

    private final ClinicaService service;

    @Operation(summary = "Listar todas las clínicas")
    @GetMapping
    public ResponseEntity<List<ClinicaResponse>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @Operation(summary = "Obtener clínica por ID")
    @GetMapping("/{id}")
    public ResponseEntity<ClinicaResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtener(id));
    }

    @Operation(summary = "Crear nueva clínica")
    @PostMapping
    public ResponseEntity<ClinicaResponse> crear(@Valid @RequestBody ClinicaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(request));
    }

    @Operation(summary = "Actualizar clínica existente")
    @PutMapping("/{id}")
    public ResponseEntity<ClinicaResponse> actualizar(@PathVariable Long id, @Valid @RequestBody ClinicaRequest request) {
        return ResponseEntity.ok(service.actualizar(id, request));
    }

    @Operation(summary = "Eliminar clínica")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
