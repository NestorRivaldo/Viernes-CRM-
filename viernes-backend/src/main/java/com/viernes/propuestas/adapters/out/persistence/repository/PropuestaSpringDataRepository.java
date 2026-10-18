package com.viernes.propuestas.adapters.out.persistence.repository;

import com.viernes.propuestas.adapters.out.persistence.entity.PropuestaEntity;
import com.viernes.propuestas.domain.EstadoPropuesta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PropuestaSpringDataRepository extends JpaRepository<PropuestaEntity, Long> {
    List<PropuestaEntity> findByIdOportunidad(Long idOportunidad);
    List<PropuestaEntity> findByIdCliente(Long idCliente);
    List<PropuestaEntity> findByEstado(EstadoPropuesta estado);
    long countByIdOportunidad(Long idOportunidad);
    long countByNumeroPropuestaStartsWith(String prefijo);
}
