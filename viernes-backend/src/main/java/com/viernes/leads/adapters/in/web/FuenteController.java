package com.viernes.leads.adapters.in.web;

import com.viernes.leads.application.dto.FuenteRequest;
import com.viernes.leads.application.dto.FuenteResponse;
import com.viernes.leads.application.port.in.ManageFuenteUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "Fuentes", description = "Catalogo de fuentes de captacion de leads")
@RestController
@RequestMapping("/api/v1/fuentes")
@RequiredArgsConstructor
public class FuenteController {

    private final ManageFuenteUseCase useCase;

    @Operation(summary = "Listar fuentes con filtro opcional por estado activa")
    @GetMapping
    public ResponseEntity<List<FuenteResponse>> listar(@RequestParam(required = false) Boolean activa) {
        return ResponseEntity.ok(useCase.listar(activa));
    }

    @Operation(summary = "Obtener fuente por ID")
    @GetMapping("/{id}")
    public ResponseEntity<FuenteResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(useCase.obtener(id));
    }

    @Operation(summary = "Crear nueva fuente")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FuenteResponse crear(@Valid @RequestBody FuenteRequest request) {
        return useCase.crear(request);
    }

    @Operation(summary = "Actualizar fuente")
    @PutMapping("/{id}")
    public ResponseEntity<FuenteResponse> actualizar(@PathVariable Long id, @Valid @RequestBody FuenteRequest request) {
        return ResponseEntity.ok(useCase.actualizar(id, request));
    }

    @Operation(summary = "Eliminar fuente")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        useCase.eliminar(id);
    }
}
