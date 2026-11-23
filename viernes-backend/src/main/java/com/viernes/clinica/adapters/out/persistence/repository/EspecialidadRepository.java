package com.viernes.clinica.adapters.out.persistence.repository;

import com.viernes.clinica.adapters.out.persistence.entity.EspecialidadEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EspecialidadRepository extends JpaRepository<EspecialidadEntity, Long> {
    Optional<EspecialidadEntity> findByCodigo(String codigo);
}
