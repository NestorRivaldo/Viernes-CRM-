package com.viernes.clinica.adapters.out.persistence.repository;

import com.viernes.clinica.adapters.out.persistence.entity.ClinicaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClinicaRepository extends JpaRepository<ClinicaEntity, Long> {
    Optional<ClinicaEntity> findByRuc(String ruc);
    boolean existsByRuc(String ruc);
    boolean existsByRucAndIdNot(String ruc, Long id);
}
