package com.viernes.oportunidades.adapters.in.web;

import com.viernes.oportunidades.application.dto.OportunidadColaboradorRequest;
import com.viernes.oportunidades.application.dto.OportunidadColaboradorResponse;
import com.viernes.oportunidades.application.dto.OportunidadContactoRequest;
import com.viernes.oportunidades.application.dto.OportunidadContactoResponse;
import com.viernes.oportunidades.application.dto.OportunidadContactoUpdateRequest;
import com.viernes.oportunidades.application.dto.OportunidadProductoRequest;
import com.viernes.oportunidades.application.dto.OportunidadProductoResponse;
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

    // --- Contactos de la oportunidad ---

    @Operation(summary = "Listar contactos asociados a la oportunidad")
    @GetMapping("/{id}/contactos")
    public ResponseEntity<List<OportunidadContactoResponse>> listarContactos(@PathVariable Long id) {
        return ResponseEntity.ok(useCase.listarContactos(id));
    }

    @Operation(summary = "Asociar un contacto a la oportunidad")
    @PostMapping("/{id}/contactos")
    @ResponseStatus(HttpStatus.CREATED)
    public OportunidadContactoResponse agregarContacto(@PathVariable Long id, @Valid @RequestBody OportunidadContactoRequest request) {
        return useCase.agregarContacto(id, request);
    }

    @Operation(summary = "Actualizar rol o marca de decisor de un contacto asociado")
    @PutMapping("/{id}/contactos/{contactoId}")
    public ResponseEntity<OportunidadContactoResponse> actualizarContacto(@PathVariable Long id, @PathVariable Long contactoId, @Valid @RequestBody OportunidadContactoUpdateRequest request) {
        return ResponseEntity.ok(useCase.actualizarContacto(id, contactoId, request));
    }

    @Operation(summary = "Quitar un contacto de la oportunidad")
    @DeleteMapping("/{id}/contactos/{contactoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void quitarContacto(@PathVariable Long id, @PathVariable Long contactoId) {
        useCase.quitarContacto(id, contactoId);
    }

    // --- Productos de la oportunidad ---

    @Operation(summary = "Listar productos asociados a la oportunidad")
    @GetMapping("/{id}/productos")
    public ResponseEntity<List<OportunidadProductoResponse>> listarProductos(@PathVariable Long id) {
        return ResponseEntity.ok(useCase.listarProductos(id));
    }

    @Operation(summary = "Asociar un producto a la oportunidad")
    @PostMapping("/{id}/productos")
    @ResponseStatus(HttpStatus.CREATED)
    public OportunidadProductoResponse agregarProducto(@PathVariable Long id, @Valid @RequestBody OportunidadProductoRequest request) {
        return useCase.agregarProducto(id, request);
    }

    @Operation(summary = "Quitar un producto de la oportunidad")
    @DeleteMapping("/{id}/productos/{productoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void quitarProducto(@PathVariable Long id, @PathVariable Long productoId) {
        useCase.quitarProducto(id, productoId);
    }

    // --- Colaboradores de la oportunidad ---

    @Operation(summary = "Listar colaboradores de la oportunidad")
    @GetMapping("/{id}/colaboradores")
    public ResponseEntity<List<OportunidadColaboradorResponse>> listarColaboradores(@PathVariable Long id) {
        return ResponseEntity.ok(useCase.listarColaboradores(id));
    }

    @Operation(summary = "Agregar un colaborador a la oportunidad")
    @PostMapping("/{id}/colaboradores")
    @ResponseStatus(HttpStatus.CREATED)
    public OportunidadColaboradorResponse agregarColaborador(@PathVariable Long id, @Valid @RequestBody OportunidadColaboradorRequest request) {
        return useCase.agregarColaborador(id, request);
    }

    @Operation(summary = "Quitar un colaborador de la oportunidad")
    @DeleteMapping("/{id}/colaboradores/{usuarioId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void quitarColaborador(@PathVariable Long id, @PathVariable Long usuarioId) {
        useCase.quitarColaborador(id, usuarioId);
    }
}
