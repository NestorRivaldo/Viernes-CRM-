package com.viernes.etiquetas.adapters.in.web;

import com.viernes.etiquetas.application.dto.EtiquetaRequest;
import com.viernes.etiquetas.application.dto.EtiquetaResponse;
import com.viernes.etiquetas.application.port.in.ManageEtiquetaUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;

@Tag(name = "Etiquetas", description = "Catálogo global de etiquetas reutilizables del CRM")
@RestController
@RequestMapping("/api/v1/etiquetas")
@RequiredArgsConstructor
public class EtiquetaController {

    private static final CacheControl CACHE_300S =
            CacheControl.maxAge(Duration.ofSeconds(300)).cachePrivate();

    private final ManageEtiquetaUseCase useCase;

    @Operation(summary = "Listar etiquetas; permite filtrar por estado y texto")
    @GetMapping
    public ResponseEntity<List<EtiquetaResponse>> listar(
            @RequestParam(required = false) Boolean activa,
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok().cacheControl(CACHE_300S).body(useCase.listar(activa, q));
    }

    @Operation(summary = "Obtener una etiqueta por ID")
    @GetMapping("/{id}")
    public ResponseEntity<EtiquetaResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok().cacheControl(CACHE_300S).body(useCase.obtener(id));
    }

    @Operation(summary = "Crear una etiqueta")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EtiquetaResponse crear(@Valid @RequestBody EtiquetaRequest request) {
        return useCase.crear(request);
    }

    @Operation(summary = "Actualizar una etiqueta")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','JEFE')")
    public ResponseEntity<EtiquetaResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody EtiquetaRequest request) {
        return ResponseEntity.ok(useCase.actualizar(id, request));
    }

    @Operation(summary = "Archivar una etiqueta; conserva el histórico de actividades")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','JEFE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        useCase.eliminar(id);
    }
}
