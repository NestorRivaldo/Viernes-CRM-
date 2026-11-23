package com.viernes.clinica.adapters.in.web;

import com.viernes.clinica.application.dto.ConsultorioRequest;
import com.viernes.clinica.application.dto.ConsultorioResponse;
import com.viernes.clinica.application.service.ConsultorioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Consultorios", description = "Gestión de Consultorios y Especialidades - LP2 Examen Variante 7")
@RestController
@RequestMapping({"/api/clinica/consultorios", "/api/v1/clinica/consultorios"})
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ConsultorioController {

    private final ConsultorioService service;

    @Operation(summary = "Listar todos los consultorios")
    @GetMapping
    public ResponseEntity<List<ConsultorioResponse>> listar() {
        return ResponseEntity.ok(service.listarConsultorios());
    }

    @Operation(summary = "Obtener detalle de consultorio con especialidades")
    @GetMapping("/{id}")
    public ResponseEntity<ConsultorioResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtenerConsultorio(id));
    }

    @Operation(summary = "Crear nuevo consultorio con asignación de especialidades")
    @PostMapping
    public ResponseEntity<ConsultorioResponse> crear(@Valid @RequestBody ConsultorioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crearConsultorio(request));
    }

    @Operation(summary = "Eliminar consultorio por ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminarConsultorio(id);
        return ResponseEntity.noContent().build();
    }
}
