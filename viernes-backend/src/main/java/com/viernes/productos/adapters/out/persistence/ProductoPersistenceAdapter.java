package com.viernes.productos.adapters.out.persistence;

import com.viernes.productos.adapters.out.persistence.entity.ProductoEntity;
import com.viernes.productos.adapters.out.persistence.repository.ProductoSpringDataRepository;
import com.viernes.productos.application.port.out.ProductoPersistencePort;
import com.viernes.productos.domain.EstadoProducto;
import com.viernes.productos.domain.Producto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ProductoPersistenceAdapter implements ProductoPersistencePort {

    private final ProductoSpringDataRepository repository;

    @Override
    public List<Producto> findAll() {
        return repository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public List<Producto> findByEstado(EstadoProducto estado) {
        return repository.findByEstado(estado).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Producto> findByNombreOrCodigo(String q) {
        return repository.findByNombreContainingIgnoreCaseOrCodigoContainingIgnoreCase(q, q)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Producto> findById(Long id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public boolean existsByCodigo(String codigo) {
        return repository.existsByCodigo(codigo);
    }

    @Override
    public Producto save(Producto p) {
        return toDomain(repository.save(toEntity(p)));
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    private Producto toDomain(ProductoEntity e) {
        return Producto.builder()
                .id(e.getId()).codigo(e.getCodigo()).nombre(e.getNombre())
                .descripcion(e.getDescripcion()).categoria(e.getCategoria())
                .precioBase(e.getPrecioBase()).estado(e.getEstado()).creadoEn(e.getCreadoEn())
                .build();
    }

    private ProductoEntity toEntity(Producto p) {
        return ProductoEntity.builder()
                .id(p.getId()).codigo(p.getCodigo()).nombre(p.getNombre())
                .descripcion(p.getDescripcion()).categoria(p.getCategoria())
                .precioBase(p.getPrecioBase()).estado(p.getEstado()).creadoEn(p.getCreadoEn())
                .build();
    }
}
