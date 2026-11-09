package com.viernes.actividades.adapters.out.persistence.repository;

import com.viernes.actividades.adapters.out.persistence.entity.ActividadEntity;
import com.viernes.actividades.domain.EstadoActividad;
import com.viernes.actividades.domain.TipoActividad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ActividadSpringDataRepository extends JpaRepository<ActividadEntity, Long> {
    List<ActividadEntity> findByEstado(EstadoActividad estado);
    List<ActividadEntity> findByIdCliente(Long idCliente);
    List<ActividadEntity> findByIdLead(Long idLead);
    List<ActividadEntity> findByIdOportunidad(Long idOportunidad);

    @Query("SELECT DISTINCT a FROM ActividadEntity a WHERE a.id IN " +
            "(SELECT ae.idActividad FROM ActividadEtiquetaEntity ae WHERE ae.idEtiqueta = :idEtiqueta)")
    List<ActividadEntity> findByEtiquetaId(@Param("idEtiqueta") Long idEtiqueta);

    List<ActividadEntity> findByIdUsuario(String idUsuario);
    List<ActividadEntity> findByTipo(TipoActividad tipo);

    /**
     * Actividades vencidas: estado = PENDIENTE y fecha anterior a la fecha actual.
     * La condición "VENCIDA" es derivada, no un estado persistido.
     */
    @Query("SELECT a FROM ActividadEntity a WHERE a.estado = :estado AND a.fecha < :ahora")
    List<ActividadEntity> findVencidas(@Param("estado") EstadoActividad estado,
                                       @Param("ahora") LocalDateTime ahora);
}
