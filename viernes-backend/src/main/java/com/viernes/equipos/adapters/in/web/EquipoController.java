package com.viernes.equipos.adapters.in.web;

import com.viernes.equipos.application.dto.EquipoRequest;
import com.viernes.equipos.application.dto.EquipoResponse;
import com.viernes.equipos.application.dto.MiembroRequest;
import com.viernes.equipos.application.dto.MiembroResponse;
import com.viernes.equipos.application.port.in.ManageEquipoUseCase;
import com.viernes.equipos.domain.EstadoEquipo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "Equipos", description = "Gestión de equipos comerciales y sus miembros")
@RestController
@RequestMapping("/api/v1/equipos")
@RequiredArgsConstructor
public class EquipoController {

    private final ManageEquipoUseCase useCase;

    @Operation(summary = "Listar equipos con filtro opcional por estado activo")
    @GetMapping
    public ResponseEntity<List<EquipoResponse>> listar(@RequestParam(required = false) Boolean activo) {
        if (activo != null) {
            return ResponseEntity.ok(useCase.listarPorEstado(activo ? EstadoEquipo.ACTIVO : EstadoEquipo.INACTIVO));
        }
        return ResponseEntity.ok(useCase.listar());
    }

    @Operation(summary = "Obtener equipo por ID")
    @GetMapping("/{id}")
    public ResponseEntity<EquipoResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(useCase.obtener(id));
    }

    @Operation(summary = "Crear nuevo equipo")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EquipoResponse crear(@Valid @RequestBody EquipoRequest request) {
        return useCase.crear(request);
    }

    @Operation(summary = "Actualizar equipo")
    @PutMapping("/{id}")
    public ResponseEntity<EquipoResponse> actualizar(@PathVariable Long id, @Valid @RequestBody EquipoRequest request) {
        return ResponseEntity.ok(useCase.actualizar(id, request));
    }

    @Operation(summary = "Activar equipo")
    @PatchMapping("/{id}/activar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void activar(@PathVariable Long id) {
        useCase.activar(id);
    }

    @Operation(summary = "Desactivar equipo")
    @PatchMapping("/{id}/desactivar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivar(@PathVariable Long id) {
        useCase.desactivar(id);
    }

    @Operation(summary = "Eliminar equipo y sus asociaciones de miembros")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        useCase.eliminar(id);
    }

    // --- Endpoints de Miembros ---

    @Operation(summary = "Listar miembros de un equipo")
    @GetMapping("/{id}/miembros")
    public ResponseEntity<List<MiembroResponse>> listarMiembros(@PathVariable Long id) {
        return ResponseEntity.ok(useCase.listarMiembros(id));
    }

    @Operation(summary = "Agregar miembro a un equipo")
    @PostMapping("/{id}/miembros")
    @ResponseStatus(HttpStatus.CREATED)
    public MiembroResponse agregarMiembro(@PathVariable Long id, @Valid @RequestBody MiembroRequest request) {
        return useCase.agregarMiembro(id, request.usuarioId());
    }

    @Operation(summary = "Quitar miembro de un equipo")
    @DeleteMapping("/{id}/miembros/{usuarioId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void quitarMiembro(@PathVariable Long id, @PathVariable Long usuarioId) {
        useCase.quitarMiembro(id, usuarioId);
    }
}
