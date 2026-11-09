package com.viernes.oportunidades.adapters.out.persistence.repository;

import com.viernes.oportunidades.adapters.out.persistence.entity.OportunidadContactoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface OportunidadContactoSpringDataRepository extends JpaRepository<OportunidadContactoEntity, Long> {
    List<OportunidadContactoEntity> findByIdOportunidad(Long idOportunidad);
    Optional<OportunidadContactoEntity> findByIdOportunidadAndIdContacto(Long idOportunidad, Long idContacto);
    boolean existsByIdOportunidadAndIdContacto(Long idOportunidad, Long idContacto);
    void deleteByIdOportunidad(Long idOportunidad);
}
