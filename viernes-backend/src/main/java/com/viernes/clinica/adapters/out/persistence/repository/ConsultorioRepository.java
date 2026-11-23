package com.viernes.clinica.adapters.out.persistence.repository;

import com.viernes.clinica.adapters.out.persistence.entity.ConsultorioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConsultorioRepository extends JpaRepository<ConsultorioEntity, Long> {
    Optional<ConsultorioEntity> findByCodigo(String codigo);
    boolean existsByCodigo(String codigo);
    boolean existsByCodigoAndIdNot(String codigo, Long id);
}
