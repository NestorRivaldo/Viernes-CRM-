package com.viernes.productos.application.port.out;

import com.viernes.productos.domain.EstadoProducto;
import com.viernes.productos.domain.Producto;
import java.util.List;
import java.util.Optional;

public interface ProductoPersistencePort {
    List<Producto> findAll();
    List<Producto> findByEstado(EstadoProducto estado);
    List<Producto> findByNombreOrCodigo(String q);
    Optional<Producto> findById(Long id);
    boolean existsByCodigo(String codigo);
    Producto save(Producto producto);
    void deleteById(Long id);
}
