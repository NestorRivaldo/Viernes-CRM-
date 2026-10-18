package com.viernes.propuestas.adapters.in.web;

import com.viernes.propuestas.application.dto.PropuestaRequest;
import com.viernes.propuestas.application.dto.PropuestaResponse;
import com.viernes.propuestas.domain.EstadoPropuesta;
import com.viernes.propuestas.application.port.in.ManagePropuestaUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "Propuestas", description = "Gestion de propuestas comerciales del CRM")
@RestController
@RequestMapping("/api/v1/propuestas")
@RequiredArgsConstructor
public class PropuestaController {

    private final ManagePropuestaUseCase useCase;

    @Operation(summary = "Crear nueva propuesta comercial")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PropuestaResponse crear(@Valid @RequestBody PropuestaRequest request) {
        return useCase.crear(request);
    }

    @Operation(summary = "Listar propuestas con filtros opcionales")
    @GetMapping
    public ResponseEntity<List<PropuestaResponse>> listar(
            @RequestParam(required = false) Long idOportunidad,
            @RequestParam(required = false) Long idCliente,
            @RequestParam(required = false) EstadoPropuesta estado) {
        if (idOportunidad != null) return ResponseEntity.ok(useCase.listarPorOportunidad(idOportunidad));
        if (idCliente != null) return ResponseEntity.ok(useCase.listarPorCliente(idCliente));
        if (estado != null) return ResponseEntity.ok(useCase.filtrarPorEstado(estado));
        return ResponseEntity.ok(useCase.listar());
    }

    @Operation(summary = "Obtener propuesta por ID")
    @GetMapping("/{id}")
    public ResponseEntity<PropuestaResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(useCase.obtener(id));
    }

    @Operation(summary = "Actualizar propuesta (solo en estado BORRADOR)")
    @PutMapping("/{id}")
    public ResponseEntity<PropuestaResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody PropuestaRequest request) {
        return ResponseEntity.ok(useCase.actualizar(id, request));
    }

    @Operation(summary = "Enviar propuesta (BORRADOR → ENVIADA)")
    @PatchMapping("/{id}/enviar")
    public ResponseEntity<PropuestaResponse> enviar(@PathVariable Long id) {
        return ResponseEntity.ok(useCase.enviar(id));
    }

    @Operation(summary = "Aprobar propuesta (ENVIADA → APROBADA)")
    @PatchMapping("/{id}/aprobar")
    public ResponseEntity<PropuestaResponse> aprobar(@PathVariable Long id) {
        return ResponseEntity.ok(useCase.aprobar(id));
    }

    @Operation(summary = "Rechazar propuesta (ENVIADA → RECHAZADA)")
    @PatchMapping("/{id}/rechazar")
    public ResponseEntity<PropuestaResponse> rechazar(@PathVariable Long id) {
        return ResponseEntity.ok(useCase.rechazar(id));
    }

    @Operation(summary = "Cancelar propuesta (desde cualquier estado no terminal)")
    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<PropuestaResponse> cancelar(@PathVariable Long id) {
        return ResponseEntity.ok(useCase.cancelar(id));
    }

    @Operation(summary = "Eliminar propuesta (solo en estado BORRADOR)")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        useCase.eliminar(id);
    }
}
