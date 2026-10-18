package com.viernes.usuarios.adapters.in.web;

import com.viernes.usuarios.application.dto.UsuarioRequest;
import com.viernes.usuarios.application.dto.UsuarioResponse;
import com.viernes.usuarios.application.port.in.ManageUsuarioUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Usuarios", description = "Gestión de usuarios del CRM")
@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final ManageUsuarioUseCase useCase;

    @Operation(summary = "Listar todos los usuarios")
    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> listar() {
        return ResponseEntity.ok(useCase.listar());
    }

    @Operation(summary = "Obtener usuario por ID")
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(useCase.obtener(id));
    }

    @Operation(summary = "Crear nuevo usuario")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse crear(@Valid @RequestBody UsuarioRequest request) {
        return useCase.crear(request);
    }

    @Operation(summary = "Actualizar usuario existente")
    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponse> actualizar(@PathVariable Long id, @Valid @RequestBody UsuarioRequest request) {
        return ResponseEntity.ok(useCase.actualizar(id, request));
    }

    @Operation(summary = "Activar usuario")
    @PatchMapping("/{id}/activar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void activar(@PathVariable Long id) {
        useCase.activar(id);
    }

    @Operation(summary = "Desactivar usuario")
    @PatchMapping("/{id}/desactivar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivar(@PathVariable Long id) {
        useCase.desactivar(id);
    }
}
