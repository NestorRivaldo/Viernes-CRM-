package com.viernes.leads.adapters.in.web;

import com.viernes.leads.application.dto.LeadRequest;
import com.viernes.leads.application.dto.LeadResponse;
import com.viernes.leads.domain.EstadoLead;
import com.viernes.leads.application.port.in.ManageLeadUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "Leads", description = "Captura y conversion de leads")
@RestController
@RequestMapping("/api/v1/leads")
@RequiredArgsConstructor
public class LeadController {

    private final ManageLeadUseCase useCase;

    @Operation(summary = "Listar leads con filtros opcionales")
    @GetMapping
    public ResponseEntity<List<LeadResponse>> listar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) EstadoLead estado) {
        if (q != null && !q.isBlank()) return ResponseEntity.ok(useCase.buscar(q));
        if (estado != null) return ResponseEntity.ok(useCase.filtrarPorEstado(estado));
        return ResponseEntity.ok(useCase.listar());
    }

    @Operation(summary = "Obtener lead por ID")
    @GetMapping("/{id}")
    public ResponseEntity<LeadResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(useCase.obtener(id));
    }

    @Operation(summary = "Crear nuevo lead")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LeadResponse crear(@Valid @RequestBody LeadRequest request) {
        return useCase.crear(request);
    }

    @Operation(summary = "Actualizar lead")
    @PutMapping("/{id}")
    public ResponseEntity<LeadResponse> actualizar(@PathVariable Long id, @Valid @RequestBody LeadRequest request) {
        return ResponseEntity.ok(useCase.actualizar(id, request));
    }

    @Operation(summary = "Cambiar estado del lead")
    @PatchMapping("/{id}/estado")
    public ResponseEntity<LeadResponse> cambiarEstado(@PathVariable Long id, @RequestParam EstadoLead estado) {
        return ResponseEntity.ok(useCase.cambiarEstado(id, estado));
    }

    @Operation(summary = "Eliminar lead")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        useCase.eliminar(id);
    }
}
