package com.viernes.mensajeria.adapters.out.persistence.repository;

import com.viernes.mensajeria.adapters.out.persistence.entity.MensajeEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MensajeSpringDataRepository extends JpaRepository<MensajeEntity, Long> {

    @Query("select m from MensajeEntity m order by m.id desc")
    List<MensajeEntity> findUltimos(Pageable pageable);

    List<MensajeEntity> findByIdGreaterThanOrderByIdAsc(Long id, Pageable pageable);

    long countByIdGreaterThan(Long id);

    @Query("select max(m.id) from MensajeEntity m")
    Long findUltimoId();
}
