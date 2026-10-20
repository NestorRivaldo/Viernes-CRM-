package com.viernes.productos.adapters.out.persistence.repository;

import com.viernes.productos.adapters.out.persistence.entity.ProductoEntity;
import com.viernes.productos.domain.EstadoProducto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoSpringDataRepository extends JpaRepository<ProductoEntity, Long> {
    List<ProductoEntity> findByEstado(EstadoProducto estado);
    List<ProductoEntity> findByNombreContainingIgnoreCaseOrCodigoContainingIgnoreCase(String nombre, String codigo);
    Optional<ProductoEntity> findByCodigo(String codigo);
    boolean existsByCodigo(String codigo);
}
