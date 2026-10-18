package com.viernes.propuestas.adapters.out.persistence;

import com.viernes.propuestas.adapters.out.persistence.entity.DetallePropuestaEntity;
import com.viernes.propuestas.adapters.out.persistence.entity.PropuestaEntity;
import com.viernes.propuestas.adapters.out.persistence.repository.PropuestaSpringDataRepository;
import com.viernes.propuestas.application.port.out.PropuestaPersistencePort;
import com.viernes.propuestas.domain.DetallePropuesta;
import com.viernes.propuestas.domain.EstadoPropuesta;
import com.viernes.propuestas.domain.Propuesta;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PropuestaPersistenceAdapter implements PropuestaPersistencePort {

    private final PropuestaSpringDataRepository repository;

    @Override
    public Propuesta save(Propuesta propuesta) {
        PropuestaEntity entity = toEntity(propuesta);
        return toDomain(repository.save(entity));
    }

    @Override
    public Optional<Propuesta> findById(Long id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Propuesta> findAll() {
        return repository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public List<Propuesta> findByIdOportunidad(Long idOportunidad) {
        return repository.findByIdOportunidad(idOportunidad).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Propuesta> findByIdCliente(Long idCliente) {
        return repository.findByIdCliente(idCliente).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Propuesta> findByEstado(EstadoPropuesta estado) {
        return repository.findByEstado(estado).stream().map(this::toDomain).toList();
    }

    @Override
    public long countByIdOportunidad(Long idOportunidad) {
        return repository.countByIdOportunidad(idOportunidad);
    }

    @Override
    public long countByNumeroPropuestaStartsWith(String prefijo) {
        return repository.countByNumeroPropuestaStartsWith(prefijo);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    // ── Mapeo Entity ↔ Domain ───────────────────────────────────────────

    private Propuesta toDomain(PropuestaEntity entity) {
        List<DetallePropuesta> detalles = entity.getDetalles().stream()
                .map(this::toDetalleDomain)
                .toList();

        return Propuesta.builder()
                .id(entity.getId())
                .numeroPropuesta(entity.getNumeroPropuesta())
                .idOportunidad(entity.getIdOportunidad())
                .idCliente(entity.getIdCliente())
                .clienteNombre(entity.getClienteNombre())
                .estado(entity.getEstado())
                .version(entity.getVersion())
                .subtotal(entity.getSubtotal())
                .descuento(entity.getDescuento())
                .total(entity.getTotal())
                .responsable(entity.getResponsable())
                .fechaEmision(entity.getFechaEmision())
                .fechaVencimiento(entity.getFechaVencimiento())
                .observaciones(entity.getObservaciones())
                .creadoEn(entity.getCreadoEn())
                .detalles(new ArrayList<>(detalles))
                .build();
    }

    private DetallePropuesta toDetalleDomain(DetallePropuestaEntity entity) {
        return DetallePropuesta.builder()
                .id(entity.getId())
                .descripcion(entity.getDescripcion())
                .cantidad(entity.getCantidad())
                .precioUnitario(entity.getPrecioUnitario())
                .subtotal(entity.getSubtotal())
                .build();
    }

    private PropuestaEntity toEntity(Propuesta domain) {
        List<DetallePropuestaEntity> detallesEntity = domain.getDetalles().stream()
                .map(this::toDetalleEntity)
                .toList();

        return PropuestaEntity.builder()
                .id(domain.getId())
                .numeroPropuesta(domain.getNumeroPropuesta())
                .idOportunidad(domain.getIdOportunidad())
                .idCliente(domain.getIdCliente())
                .clienteNombre(domain.getClienteNombre())
                .estado(domain.getEstado())
                .version(domain.getVersion())
                .subtotal(domain.getSubtotal())
                .descuento(domain.getDescuento())
                .total(domain.getTotal())
                .responsable(domain.getResponsable())
                .fechaEmision(domain.getFechaEmision())
                .fechaVencimiento(domain.getFechaVencimiento())
                .observaciones(domain.getObservaciones())
                .creadoEn(domain.getCreadoEn())
                .detalles(new ArrayList<>(detallesEntity))
                .build();
    }

    private DetallePropuestaEntity toDetalleEntity(DetallePropuesta domain) {
        return DetallePropuestaEntity.builder()
                .id(domain.getId())
                .descripcion(domain.getDescripcion())
                .cantidad(domain.getCantidad())
                .precioUnitario(domain.getPrecioUnitario())
                .subtotal(domain.getSubtotal())
                .build();
    }
}
