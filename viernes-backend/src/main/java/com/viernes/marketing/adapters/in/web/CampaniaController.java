package com.viernes.marketing.adapters.in.web;

import com.viernes.marketing.application.dto.CampaniaLeadRequest;
import com.viernes.marketing.application.dto.CampaniaLeadResponse;
import com.viernes.marketing.application.dto.CampaniaRequest;
import com.viernes.marketing.application.dto.CampaniaResponse;
import com.viernes.marketing.application.port.in.ManageCampaniaUseCase;
import com.viernes.marketing.domain.EstadoCampania;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "Campanias", description = "Gestion de campanias de marketing")
@RestController
@RequestMapping("/api/v1/campanias")
@RequiredArgsConstructor
public class CampaniaController {

    private final ManageCampaniaUseCase useCase;

    @Operation(summary = "Listar campanias")
    @GetMapping
    public ResponseEntity<List<CampaniaResponse>> listar() {
        return ResponseEntity.ok(useCase.listar());
    }

    @Operation(summary = "Obtener campania por ID")
    @GetMapping("/{id}")
    public ResponseEntity<CampaniaResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(useCase.obtener(id));
    }

    @Operation(summary = "Crear nueva campania")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CampaniaResponse crear(@Valid @RequestBody CampaniaRequest request) {
        return useCase.crear(request);
    }

    @Operation(summary = "Actualizar campania")
    @PutMapping("/{id}")
    public ResponseEntity<CampaniaResponse> actualizar(@PathVariable Long id, @Valid @RequestBody CampaniaRequest request) {
        return ResponseEntity.ok(useCase.actualizar(id, request));
    }

    @Operation(summary = "Cambiar estado de la campania")
    @PatchMapping("/{id}/estado")
    public ResponseEntity<CampaniaResponse> cambiarEstado(@PathVariable Long id, @RequestParam EstadoCampania estado) {
        return ResponseEntity.ok(useCase.cambiarEstado(id, estado));
    }

    @Operation(summary = "Eliminar campania")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        useCase.eliminar(id);
    }

    @Operation(summary = "Listar leads asociados a la campania")
    @GetMapping("/{id}/leads")
    public ResponseEntity<List<CampaniaLeadResponse>> listarLeads(@PathVariable Long id) {
        return ResponseEntity.ok(useCase.listarLeads(id));
    }

    @Operation(summary = "Asociar lead a la campania")
    @PostMapping("/{id}/leads")
    @ResponseStatus(HttpStatus.CREATED)
    public CampaniaLeadResponse asociarLead(@PathVariable Long id, @Valid @RequestBody CampaniaLeadRequest request) {
        return useCase.asociarLead(id, request.leadId());
    }

    @Operation(summary = "Desasociar lead de la campania")
    @DeleteMapping("/{id}/leads/{leadId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desasociarLead(@PathVariable Long id, @PathVariable Long leadId) {
        useCase.desasociarLead(id, leadId);
    }
}
