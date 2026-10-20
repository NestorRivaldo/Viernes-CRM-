package com.viernes.productos.adapters.in.web;

import com.viernes.productos.application.dto.ProductoRequest;
import com.viernes.productos.application.dto.ProductoResponse;
import com.viernes.productos.domain.EstadoProducto;
import com.viernes.productos.application.port.in.ManageProductoUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "Productos", description = "Catalogo de productos y servicios")
@RestController
@RequestMapping("/api/v1/productos")
@RequiredArgsConstructor
public class ProductoController {

    private final ManageProductoUseCase useCase;

    @Operation(summary = "Listar productos con filtros opcionales")
    @GetMapping
    public ResponseEntity<List<ProductoResponse>> listar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) EstadoProducto estado) {
        if (q != null && !q.isBlank()) return ResponseEntity.ok(useCase.buscar(q));
        if (estado != null) return ResponseEntity.ok(useCase.listarPorEstado(estado));
        return ResponseEntity.ok(useCase.listar());
    }

    @Operation(summary = "Obtener producto por ID")
    @GetMapping("/{id}")
    public ResponseEntity<ProductoResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(useCase.obtener(id));
    }

    @Operation(summary = "Crear nuevo producto")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductoResponse crear(@Valid @RequestBody ProductoRequest request) {
        return useCase.crear(request);
    }

    @Operation(summary = "Actualizar producto")
    @PutMapping("/{id}")
    public ResponseEntity<ProductoResponse> actualizar(@PathVariable Long id, @Valid @RequestBody ProductoRequest request) {
        return ResponseEntity.ok(useCase.actualizar(id, request));
    }

    @Operation(summary = "Eliminar producto")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        useCase.eliminar(id);
    }
}
