package com.viernes.oportunidades.adapters.in.web;

import com.viernes.oportunidades.application.dto.OportunidadRequest;
import com.viernes.oportunidades.application.dto.OportunidadResponse;
import com.viernes.oportunidades.domain.EtapaOportunidad;
import com.viernes.oportunidades.application.port.in.ManageOportunidadUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "Oportunidades", description = "Gestion de oportunidades de venta")
@RestController
@RequestMapping("/api/v1/oportunidades")
@RequiredArgsConstructor
public class OportunidadController {

    private final ManageOportunidadUseCase useCase;

    @Operation(summary = "Listar oportunidades con filtros opcionales")
    @GetMapping
    public ResponseEntity<List<OportunidadResponse>> listar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) EtapaOportunidad etapa) {
        if (q != null && !q.isBlank()) return ResponseEntity.ok(useCase.buscar(q));
        if (etapa != null) return ResponseEntity.ok(useCase.filtrarPorEtapa(etapa));
        return ResponseEntity.ok(useCase.listar());
    }

    @Operation(summary = "Obtener oportunidad por ID")
    @GetMapping("/{id}")
    public ResponseEntity<OportunidadResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(useCase.obtener(id));
    }

    @Operation(summary = "Crear nueva oportunidad")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OportunidadResponse crear(@Valid @RequestBody OportunidadRequest request) {
        return useCase.crear(request);
    }

    @Operation(summary = "Actualizar oportunidad")
    @PutMapping("/{id}")
    public ResponseEntity<OportunidadResponse> actualizar(@PathVariable Long id, @Valid @RequestBody OportunidadRequest request) {
        return ResponseEntity.ok(useCase.actualizar(id, request));
    }

    @Operation(summary = "Cambiar etapa de la oportunidad")
    @PatchMapping("/{id}/etapa")
    public ResponseEntity<OportunidadResponse> cambiarEtapa(@PathVariable Long id, @RequestParam EtapaOportunidad etapa) {
        return ResponseEntity.ok(useCase.cambiarEtapa(id, etapa));
    }

    @Operation(summary = "Eliminar oportunidad")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        useCase.eliminar(id);
    }
}
