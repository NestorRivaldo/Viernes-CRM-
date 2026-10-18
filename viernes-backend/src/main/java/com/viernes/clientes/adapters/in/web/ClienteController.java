package com.viernes.clientes.adapters.in.web;

import com.viernes.clientes.application.dto.ClienteRequest;
import com.viernes.clientes.application.dto.ClienteResponse;
import com.viernes.clientes.application.dto.ContactoRequest;
import com.viernes.clientes.application.dto.ContactoResponse;
import com.viernes.clientes.domain.EstadoCliente;
import com.viernes.clientes.application.port.in.ManageClienteUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "Clientes", description = "Gestion de clientes del CRM")
@RestController
@RequestMapping("/api/v1/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ManageClienteUseCase useCase;

    @Operation(summary = "Listar todos los clientes")
    @GetMapping
    public ResponseEntity<List<ClienteResponse>> listar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) EstadoCliente estado) {
        if (q != null && !q.isBlank()) return ResponseEntity.ok(useCase.buscar(q));
        if (estado != null) return ResponseEntity.ok(useCase.filtrarPorEstado(estado));
        return ResponseEntity.ok(useCase.listar());
    }

    @Operation(summary = "Obtener cliente por ID")
    @GetMapping("/{id}")
    public ResponseEntity<ClienteResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(useCase.obtener(id));
    }

    @Operation(summary = "Crear nuevo cliente")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClienteResponse crear(@Valid @RequestBody ClienteRequest request) {
        return useCase.crear(request);
    }

    @Operation(summary = "Actualizar cliente existente")
    @PutMapping("/{id}")
    public ResponseEntity<ClienteResponse> actualizar(@PathVariable Long id, @Valid @RequestBody ClienteRequest request) {
        return ResponseEntity.ok(useCase.actualizar(id, request));
    }

    @Operation(summary = "Desactivar cliente")
    @PatchMapping("/{id}/desactivar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivar(@PathVariable Long id) {
        useCase.desactivar(id);
    }

    @Operation(summary = "Eliminar cliente")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        useCase.eliminar(id);
    }

    // --- Endpoints de Contactos ---

    @Operation(summary = "Listar contactos de un cliente")
    @GetMapping("/{clienteId}/contactos")
    public ResponseEntity<List<ContactoResponse>> listarContactos(@PathVariable Long clienteId) {
        return ResponseEntity.ok(useCase.listarContactos(clienteId));
    }

    @Operation(summary = "Agregar contacto a un cliente")
    @PostMapping("/{clienteId}/contactos")
    @ResponseStatus(HttpStatus.CREATED)
    public ContactoResponse agregarContacto(@PathVariable Long clienteId, @Valid @RequestBody ContactoRequest request) {
        return useCase.agregarContacto(clienteId, request);
    }

    @Operation(summary = "Actualizar contacto de un cliente")
    @PutMapping("/{clienteId}/contactos/{contactoId}")
    public ResponseEntity<ContactoResponse> actualizarContacto(
            @PathVariable Long clienteId, 
            @PathVariable Long contactoId, 
            @Valid @RequestBody ContactoRequest request) {
        return ResponseEntity.ok(useCase.actualizarContacto(clienteId, contactoId, request));
    }

    @Operation(summary = "Eliminar contacto de un cliente")
    @DeleteMapping("/{clienteId}/contactos/{contactoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarContacto(@PathVariable Long clienteId, @PathVariable Long contactoId) {
        useCase.eliminarContacto(clienteId, contactoId);
    }
}
