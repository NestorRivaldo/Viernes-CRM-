package com.viernes.actividades.adapters.in.web;

import com.viernes.actividades.application.dto.ActividadRequest;
import com.viernes.actividades.application.dto.ActividadResponse;
import com.viernes.actividades.application.dto.ParticipanteRequest;
import com.viernes.actividades.application.dto.ParticipanteResponse;
import com.viernes.actividades.application.port.in.ManageActividadUseCase;
import com.viernes.actividades.domain.EstadoActividad;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/actividades")
public class ActividadController {

    private final ManageActividadUseCase useCase;

    public ActividadController(ManageActividadUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping
    public ResponseEntity<ActividadResponse> crear(@Valid @RequestBody ActividadRequest request) {
        ActividadResponse response = useCase.crear(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ActividadResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(useCase.obtener(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ActividadResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ActividadRequest request) {
        return ResponseEntity.ok(useCase.actualizar(id, request));
    }

    @GetMapping
    public ResponseEntity<List<ActividadResponse>> listar(
            @RequestParam(required = false) EstadoActividad estado,
            @RequestParam(required = false) Long idCliente,
            @RequestParam(required = false) Long idLead,
            @RequestParam(required = false) Long idOportunidad,
            @RequestParam(required = false) String idUsuario,
            @RequestParam(required = false) Boolean vencidas
    ) {
        if (Boolean.TRUE.equals(vencidas)) {
            return ResponseEntity.ok(useCase.listarVencidas());
        } else if (estado != null) {
            return ResponseEntity.ok(useCase.filtrarPorEstado(estado));
        } else if (idCliente != null) {
            return ResponseEntity.ok(useCase.filtrarPorCliente(idCliente));
        } else if (idLead != null) {
            return ResponseEntity.ok(useCase.filtrarPorLead(idLead));
        } else if (idOportunidad != null) {
            return ResponseEntity.ok(useCase.filtrarPorOportunidad(idOportunidad));
        } else if (idUsuario != null) {
            return ResponseEntity.ok(useCase.filtrarPorUsuario(idUsuario));
        }
        
        return ResponseEntity.ok(useCase.listarTodos());
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<ActividadResponse> cambiarEstado(
            @PathVariable Long id,
            @RequestParam EstadoActividad estado
    ) {
        return ResponseEntity.ok(useCase.cambiarEstado(id, estado));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        useCase.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    // --- Endpoints de Participantes ---

    @GetMapping("/{id}/participantes")
    public ResponseEntity<List<ParticipanteResponse>> listarParticipantes(@PathVariable Long id) {
        return ResponseEntity.ok(useCase.listarParticipantes(id));
    }

    @PostMapping("/{id}/participantes")
    public ResponseEntity<ParticipanteResponse> agregarParticipante(
            @PathVariable Long id,
            @Valid @RequestBody ParticipanteRequest request) {
        ParticipanteResponse response = useCase.agregarParticipante(id, request.contactoId());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}/participantes/{contactoId}")
    public ResponseEntity<Void> quitarParticipante(@PathVariable Long id, @PathVariable Long contactoId) {
        useCase.quitarParticipante(id, contactoId);
        return ResponseEntity.noContent().build();
    }
}
