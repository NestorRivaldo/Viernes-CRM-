package com.viernes.propuestas.domain;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Propuesta {
    private Long id;
    private String numeroPropuesta;
    private Long idOportunidad;
    private Long idCliente;
    private String clienteNombre;
    private EstadoPropuesta estado;
    @Builder.Default
    private Integer version = 1;
    private BigDecimal subtotal;
    @Builder.Default
    private BigDecimal descuento = BigDecimal.ZERO;
    private BigDecimal total;
    private String responsable;
    private LocalDate fechaEmision;
    private LocalDate fechaVencimiento;
    private String observaciones;
    private LocalDateTime creadoEn;
    @Builder.Default
    private List<DetallePropuesta> detalles = new ArrayList<>();

    /**
     * Recalcula subtotal y total a partir de los detalles.
     */
    public void recalcularTotales() {
        this.subtotal = detalles.stream()
                .map(DetallePropuesta::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.total = this.subtotal.subtract(this.descuento != null ? this.descuento : BigDecimal.ZERO);
    }

    /**
     * Verifica si la propuesta está en un estado terminal (no modificable).
     */
    public boolean esEstadoTerminal() {
        return estado == EstadoPropuesta.APROBADA
                || estado == EstadoPropuesta.RECHAZADA
                || estado == EstadoPropuesta.CANCELADA;
    }
}
