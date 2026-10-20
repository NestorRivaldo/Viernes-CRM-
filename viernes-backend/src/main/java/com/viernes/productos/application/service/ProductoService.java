package com.viernes.productos.application.service;

import com.viernes.productos.application.dto.ProductoRequest;
import com.viernes.productos.application.dto.ProductoResponse;
import com.viernes.productos.application.port.in.ManageProductoUseCase;
import com.viernes.productos.application.port.out.ProductoPersistencePort;
import com.viernes.productos.domain.EstadoProducto;
import com.viernes.productos.domain.Producto;
import com.viernes.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductoService implements ManageProductoUseCase {

    private final ProductoPersistencePort persistencePort;

    @Override
    public List<ProductoResponse> listar() {
        return persistencePort.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public List<ProductoResponse> buscar(String q) {
        return persistencePort.findByNombreOrCodigo(q).stream().map(this::toResponse).toList();
    }

    @Override
    public List<ProductoResponse> listarPorEstado(EstadoProducto estado) {
        return persistencePort.findByEstado(estado).stream().map(this::toResponse).toList();
    }

    @Override
    public ProductoResponse obtener(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    @Transactional
    public ProductoResponse crear(ProductoRequest request) {
        if (persistencePort.existsByCodigo(request.codigo())) {
            throw new IllegalArgumentException("Ya existe un producto con el código: " + request.codigo());
        }
        Producto p = Producto.builder()
                .creadoEn(LocalDateTime.now())
                .estado(request.estado() != null ? request.estado() : EstadoProducto.ACTIVO)
                .build();
        apply(p, request);
        return toResponse(persistencePort.save(p));
    }

    @Override
    @Transactional
    public ProductoResponse actualizar(Long id, ProductoRequest request) {
        Producto p = findOrThrow(id);
        apply(p, request);
        return toResponse(persistencePort.save(p));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        findOrThrow(id);
        persistencePort.deleteById(id);
    }

    private Producto findOrThrow(Long id) {
        return persistencePort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + id));
    }

    private void apply(Producto p, ProductoRequest r) {
        p.setCodigo(r.codigo());
        p.setNombre(r.nombre());
        p.setDescripcion(r.descripcion());
        p.setCategoria(r.categoria());
        p.setPrecioBase(r.precioBase());
        if (r.estado() != null) p.setEstado(r.estado());
    }

    private ProductoResponse toResponse(Producto p) {
        return new ProductoResponse(p.getId(), p.getCodigo(), p.getNombre(), p.getDescripcion(),
                p.getCategoria(), p.getPrecioBase(), p.getEstado(), p.getCreadoEn());
    }
}
