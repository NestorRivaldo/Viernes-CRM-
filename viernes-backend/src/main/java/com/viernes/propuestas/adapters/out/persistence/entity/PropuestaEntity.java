package com.viernes.propuestas.adapters.out.persistence.entity;

import com.viernes.propuestas.domain.EstadoPropuesta;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "CRM_PROPUESTAS")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PropuestaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "NUMERO_PROPUESTA", nullable = false, unique = true, length = 50)
    private String numeroPropuesta;

    @Column(name = "ID_OPORTUNIDAD", nullable = false)
    private Long idOportunidad;

    @Column(name = "ID_CLIENTE", nullable = false)
    private Long idCliente;

    @Column(name = "CLIENTE_NOMBRE", length = 150)
    private String clienteNombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO", nullable = false, length = 20)
    @Builder.Default
    private EstadoPropuesta estado = EstadoPropuesta.BORRADOR;

    @Column(name = "VERSION", nullable = false)
    @Builder.Default
    private Integer version = 1;

    @Column(name = "SUBTOTAL", nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "DESCUENTO", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal descuento = BigDecimal.ZERO;

    @Column(name = "TOTAL", nullable = false, precision = 14, scale = 2)
    private BigDecimal total;

    @Column(name = "RESPONSABLE", length = 100)
    private String responsable;

    @Column(name = "FECHA_EMISION", nullable = false)
    private LocalDate fechaEmision;

    @Column(name = "FECHA_VENCIMIENTO")
    private LocalDate fechaVencimiento;

    @Lob
    @Column(name = "OBSERVACIONES")
    private String observaciones;

    @Column(name = "CREADO_EN", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime creadoEn = LocalDateTime.now();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "ID_PROPUESTA", nullable = false)
    @Builder.Default
    private List<DetallePropuestaEntity> detalles = new ArrayList<>();
}
