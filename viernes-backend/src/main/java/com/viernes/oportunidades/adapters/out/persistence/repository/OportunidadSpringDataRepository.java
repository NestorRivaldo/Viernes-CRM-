package com.viernes.oportunidades.adapters.out.persistence.repository;

import com.viernes.oportunidades.adapters.out.persistence.entity.OportunidadEntity;
import com.viernes.oportunidades.domain.EtapaOportunidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface OportunidadSpringDataRepository extends JpaRepository<OportunidadEntity, Long> {
    List<OportunidadEntity> findByEtapa(EtapaOportunidad etapa);
    List<OportunidadEntity> findByNombreContainingIgnoreCaseOrClienteNombreContainingIgnoreCase(String nombre, String cliente);
    long countByEtapa(EtapaOportunidad etapa);
    List<OportunidadEntity> findByFechaCierreBeforeAndEtapaNotIn(LocalDate fecha, List<EtapaOportunidad> etapas);
    @Query("SELECT COALESCE(SUM(o.valor * o.probabilidad / 100), 0) FROM OportunidadEntity o WHERE o.etapa NOT IN ('GANADA','PERDIDA')")
    BigDecimal calcularPipeline();
}
