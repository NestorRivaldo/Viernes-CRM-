package com.viernes.oportunidades.adapters.out.persistence.repository;

import com.viernes.oportunidades.adapters.out.persistence.entity.OportunidadProductoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface OportunidadProductoSpringDataRepository extends JpaRepository<OportunidadProductoEntity, Long> {
    List<OportunidadProductoEntity> findByIdOportunidad(Long idOportunidad);
    Optional<OportunidadProductoEntity> findByIdOportunidadAndIdProducto(Long idOportunidad, Long idProducto);
    boolean existsByIdOportunidadAndIdProducto(Long idOportunidad, Long idProducto);
    void deleteByIdOportunidad(Long idOportunidad);
}
