package com.viernes.propuestas.application.service;

import com.viernes.propuestas.application.dto.*;
import com.viernes.propuestas.application.port.in.ManagePropuestaUseCase;
import com.viernes.propuestas.application.port.out.PropuestaPersistencePort;
import com.viernes.propuestas.application.port.out.PropuestaRelacionValidationPort;
import com.viernes.propuestas.domain.DetallePropuesta;
import com.viernes.propuestas.domain.EstadoPropuesta;
import com.viernes.propuestas.domain.Propuesta;
import com.viernes.productos.application.dto.ProductoResponse;
import com.viernes.productos.application.port.in.ManageProductoUseCase;
import com.viernes.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PropuestaService implements ManagePropuestaUseCase {

    private final PropuestaPersistencePort persistencePort;
    private final PropuestaRelacionValidationPort validationPort;
    private final ManageProductoUseCase productoUseCase;

    @Override
    @Transactional
    public PropuestaResponse crear(PropuestaRequest request) {
        validarRelaciones(request);
        validarFechas(request);

        Propuesta propuesta = Propuesta.builder()
                .numeroPropuesta(generarNumeroPropuesta())
                .idOportunidad(request.idOportunidad())
                .idCliente(request.idCliente())
                .clienteNombre(request.clienteNombre())
                .estado(EstadoPropuesta.BORRADOR)
                .version(1)
                .descuento(request.descuento() != null ? request.descuento() : BigDecimal.ZERO)
                .responsable(request.responsable())
                .fechaEmision(request.fechaEmision())
                .fechaVencimiento(request.fechaVencimiento())
                .observaciones(request.observaciones())
                .creadoEn(LocalDateTime.now())
                .build();

        List<DetallePropuesta> detalles = request.detalles().stream()
                .map(this::toDetalleDomain)
                .toList();
        propuesta.setDetalles(new java.util.ArrayList<>(detalles));
        propuesta.recalcularTotales();

        return toResponse(persistencePort.save(propuesta));
    }

    @Override
    @Transactional(readOnly = true)
    public PropuestaResponse obtener(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PropuestaResponse> listar() {
        return persistencePort.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PropuestaResponse> listarPorOportunidad(Long idOportunidad) {
        return persistencePort.findByIdOportunidad(idOportunidad).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PropuestaResponse> listarPorCliente(Long idCliente) {
        return persistencePort.findByIdCliente(idCliente).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PropuestaResponse> filtrarPorEstado(EstadoPropuesta estado) {
        return persistencePort.findByEstado(estado).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public PropuestaResponse actualizar(Long id, PropuestaRequest request) {
        Propuesta propuesta = findOrThrow(id);
        validarModificable(propuesta, "actualizar");
        validarRelaciones(request);
        validarFechas(request);

        propuesta.setIdOportunidad(request.idOportunidad());
        propuesta.setIdCliente(request.idCliente());
        propuesta.setClienteNombre(request.clienteNombre());
        propuesta.setDescuento(request.descuento() != null ? request.descuento() : BigDecimal.ZERO);
        propuesta.setResponsable(request.responsable());
        propuesta.setFechaEmision(request.fechaEmision());
        propuesta.setFechaVencimiento(request.fechaVencimiento());
        propuesta.setObservaciones(request.observaciones());

        List<DetallePropuesta> detalles = request.detalles().stream()
                .map(this::toDetalleDomain)
                .toList();
        propuesta.setDetalles(new java.util.ArrayList<>(detalles));
        propuesta.recalcularTotales();

        return toResponse(persistencePort.save(propuesta));
    }

    @Override
    @Transactional
    public PropuestaResponse enviar(Long id) {
        Propuesta propuesta = findOrThrow(id);
        validarTransicion(propuesta, EstadoPropuesta.BORRADOR, "enviar");
        propuesta.setEstado(EstadoPropuesta.ENVIADA);
        return toResponse(persistencePort.save(propuesta));
    }

    @Override
    @Transactional
    public PropuestaResponse aprobar(Long id) {
        Propuesta propuesta = findOrThrow(id);
        validarTransicion(propuesta, EstadoPropuesta.ENVIADA, "aprobar");
        propuesta.setEstado(EstadoPropuesta.APROBADA);
        return toResponse(persistencePort.save(propuesta));
    }

    @Override
    @Transactional
    public PropuestaResponse rechazar(Long id) {
        Propuesta propuesta = findOrThrow(id);
        validarTransicion(propuesta, EstadoPropuesta.ENVIADA, "rechazar");
        propuesta.setEstado(EstadoPropuesta.RECHAZADA);
        return toResponse(persistencePort.save(propuesta));
    }

    @Override
    @Transactional
    public PropuestaResponse cancelar(Long id) {
        Propuesta propuesta = findOrThrow(id);
        if (propuesta.esEstadoTerminal()) {
            throw new IllegalStateException(
                    "No se puede cancelar una propuesta en estado " + propuesta.getEstado());
        }
        propuesta.setEstado(EstadoPropuesta.CANCELADA);
        return toResponse(persistencePort.save(propuesta));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Propuesta propuesta = findOrThrow(id);
        validarModificable(propuesta, "eliminar");
        persistencePort.deleteById(id);
    }

    // ── Helpers privados ────────────────────────────────────────────────

    private Propuesta findOrThrow(Long id) {
        return persistencePort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Propuesta no encontrada: " + id));
    }

    private void validarRelaciones(PropuestaRequest request) {
        if (!validationPort.existeOportunidad(request.idOportunidad())) {
            throw new ResourceNotFoundException("Oportunidad no encontrada: " + request.idOportunidad());
        }
        if (!validationPort.existeCliente(request.idCliente())) {
            throw new ResourceNotFoundException("Cliente no encontrado: " + request.idCliente());
        }
    }

    private void validarFechas(PropuestaRequest request) {
        if (request.fechaVencimiento() != null
                && request.fechaVencimiento().isBefore(request.fechaEmision())) {
            throw new IllegalArgumentException(
                    "La fecha de vencimiento no puede ser anterior a la fecha de emisión");
        }
    }

    private void validarModificable(Propuesta propuesta, String accion) {
        if (propuesta.getEstado() != EstadoPropuesta.BORRADOR) {
            throw new IllegalStateException(
                    "No se puede " + accion + " una propuesta en estado " + propuesta.getEstado()
                    + ". Solo se permite en estado BORRADOR");
        }
    }

    private void validarTransicion(Propuesta propuesta, EstadoPropuesta estadoRequerido, String accion) {
        if (propuesta.getEstado() != estadoRequerido) {
            throw new IllegalStateException(
                    "No se puede " + accion + " una propuesta en estado " + propuesta.getEstado()
                    + ". Estado requerido: " + estadoRequerido);
        }
    }

    /**
     * Correlativo global por año. La unicidad final la garantiza el constraint
     * UNIQUE de NUMERO_PROPUESTA; una colisión concurrente termina en 409 y el
     * cliente puede reintentar. Para alta concurrencia migrar a una secuencia
     * Oracle (deuda técnica documentada).
     */
    private String generarNumeroPropuesta() {
        String prefijo = String.format("PROP-%d-", Year.now().getValue());
        long count = persistencePort.countByNumeroPropuestaStartsWith(prefijo);
        return String.format("%s%04d", prefijo, count + 1);
    }

    /**
     * Construye el detalle guardando un snapshot: si trae productoId se valida
     * contra el catálogo y los campos vacíos se prellenan desde el producto;
     * los valores enviados se respetan como overrides comerciales.
     */
    private DetallePropuesta toDetalleDomain(DetallePropuestaRequest r) {
        String descripcion = r.descripcion();
        BigDecimal precioUnitario = r.precioUnitario();

        if (r.productoId() != null) {
            ProductoResponse producto = productoUseCase.obtener(r.productoId());
            if (descripcion == null || descripcion.isBlank()) {
                descripcion = producto.nombre() != null && !producto.nombre().isBlank()
                        ? producto.nombre()
                        : producto.descripcion();
            }
            if (precioUnitario == null) {
                precioUnitario = producto.precioBase();
            }
        }

        if (descripcion == null || descripcion.isBlank()) {
            throw new IllegalArgumentException("La descripción del detalle es obligatoria");
        }
        if (precioUnitario == null) {
            throw new IllegalArgumentException("El precio unitario es obligatorio");
        }

        DetallePropuesta d = DetallePropuesta.builder()
                .idProducto(r.productoId())
                .descripcion(descripcion)
                .cantidad(r.cantidad())
                .precioUnitario(precioUnitario)
                .build();
        d.calcularSubtotal();
        return d;
    }

    private PropuestaResponse toResponse(Propuesta p) {
        List<DetallePropuestaResponse> detallesResp = p.getDetalles().stream()
                .map(d -> new DetallePropuestaResponse(
                        d.getId(), d.getIdProducto(), d.getDescripcion(), d.getCantidad(),
                        d.getPrecioUnitario(), d.getSubtotal()))
                .toList();

        return new PropuestaResponse(
                p.getId(), p.getNumeroPropuesta(), p.getIdOportunidad(),
                p.getIdCliente(), p.getClienteNombre(), p.getEstado(),
                p.getVersion(), p.getSubtotal(), p.getDescuento(), p.getTotal(),
                p.getResponsable(), p.getFechaEmision(), p.getFechaVencimiento(),
                p.getObservaciones(), p.getCreadoEn(), detallesResp);
    }
}
