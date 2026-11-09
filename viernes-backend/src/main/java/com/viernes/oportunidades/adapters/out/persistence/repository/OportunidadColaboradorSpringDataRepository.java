package com.viernes.oportunidades.adapters.out.persistence.repository;

import com.viernes.oportunidades.adapters.out.persistence.entity.OportunidadColaboradorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface OportunidadColaboradorSpringDataRepository extends JpaRepository<OportunidadColaboradorEntity, Long> {
    List<OportunidadColaboradorEntity> findByIdOportunidad(Long idOportunidad);
    Optional<OportunidadColaboradorEntity> findByIdOportunidadAndIdUsuario(Long idOportunidad, Long idUsuario);
    boolean existsByIdOportunidadAndIdUsuario(Long idOportunidad, Long idUsuario);
    void deleteByIdOportunidad(Long idOportunidad);
}
