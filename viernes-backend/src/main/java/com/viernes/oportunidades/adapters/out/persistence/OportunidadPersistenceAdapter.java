package com.viernes.oportunidades.adapters.out.persistence;

import com.viernes.oportunidades.adapters.out.persistence.entity.OportunidadEntity;
import com.viernes.oportunidades.adapters.out.persistence.repository.OportunidadSpringDataRepository;
import com.viernes.oportunidades.application.port.out.OportunidadPersistencePort;
import com.viernes.oportunidades.domain.EtapaOportunidad;
import com.viernes.oportunidades.domain.Oportunidad;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OportunidadPersistenceAdapter implements OportunidadPersistencePort {

    private final OportunidadSpringDataRepository repository;

    @Override
    public List<Oportunidad> findAll() {
        return repository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public List<Oportunidad> findByNombreOrClienteNombre(String q) {
        return repository.findByNombreContainingIgnoreCaseOrClienteNombreContainingIgnoreCase(q, q)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<Oportunidad> findByEtapa(EtapaOportunidad etapa) {
        return repository.findByEtapa(etapa).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Oportunidad> findByFechaCierreBeforeAndEtapaNotIn(LocalDate fecha, List<EtapaOportunidad> etapas) {
        return repository.findByFechaCierreBeforeAndEtapaNotIn(fecha, etapas)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public long count() {
        return repository.count();
    }

    @Override
    public long countByEtapa(EtapaOportunidad etapa) {
        return repository.countByEtapa(etapa);
    }

    @Override
    public BigDecimal calcularPipeline() {
        return repository.calcularPipeline();
    }

    @Override
    public Optional<Oportunidad> findById(Long id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Oportunidad save(Oportunidad oportunidad) {
        OportunidadEntity entity = toEntity(oportunidad);
        return toDomain(repository.save(entity));
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    private Oportunidad toDomain(OportunidadEntity entity) {
        return Oportunidad.builder()
                .id(entity.getId())
                .nombre(entity.getNombre())
                .clienteNombre(entity.getClienteNombre())
                .valor(entity.getValor())
                .probabilidad(entity.getProbabilidad())
                .etapa(entity.getEtapa())
                .responsable(entity.getResponsable())
                .fechaCierre(entity.getFechaCierre())
                .creadoEn(entity.getCreadoEn())
                .build();
    }

    private OportunidadEntity toEntity(Oportunidad domain) {
        return OportunidadEntity.builder()
                .id(domain.getId())
                .nombre(domain.getNombre())
                .clienteNombre(domain.getClienteNombre())
                .valor(domain.getValor())
                .probabilidad(domain.getProbabilidad())
                .etapa(domain.getEtapa())
                .responsable(domain.getResponsable())
                .fechaCierre(domain.getFechaCierre())
                .creadoEn(domain.getCreadoEn())
                .build();
    }
}
