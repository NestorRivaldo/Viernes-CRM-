package com.viernes.oportunidades.application.port.out;

import com.viernes.oportunidades.domain.EtapaOportunidad;
import com.viernes.oportunidades.domain.Oportunidad;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface OportunidadPersistencePort {
    List<Oportunidad> findAll();
    List<Oportunidad> findByNombreOrClienteNombre(String q);
    List<Oportunidad> findByEtapa(EtapaOportunidad etapa);
    List<Oportunidad> findByFechaCierreBeforeAndEtapaNotIn(LocalDate fecha, List<EtapaOportunidad> etapas);
    long count();
    long countByEtapa(EtapaOportunidad etapa);
    BigDecimal calcularPipeline();
    Optional<Oportunidad> findById(Long id);
    Oportunidad save(Oportunidad oportunidad);
    void deleteById(Long id);
}
