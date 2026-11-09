package com.viernes.oportunidades.adapters.out.persistence.entity;

import com.viernes.oportunidades.domain.EtapaOportunidad;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "CRM_OPORTUNIDADES")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OportunidadEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "NOMBRE", nullable = false, length = 200)
    private String nombre;

    @Column(name = "CLIENTE_NOMBRE", length = 150)
    private String clienteNombre;

    @Column(name = "VALOR", precision = 14, scale = 2)
    private BigDecimal valor;

    @Column(name = "PROBABILIDAD")
    private Integer probabilidad; // 0-100

    @Enumerated(EnumType.STRING)
    @Column(name = "ETAPA", nullable = false, length = 20)
    @Builder.Default
    private EtapaOportunidad etapa = EtapaOportunidad.PROSPECCION;

    @Column(name = "RESPONSABLE", length = 100)
    private String responsable;

    @Column(name = "FECHA_CIERRE")
    private LocalDate fechaCierre;

    @Column(name = "DESCRIPCION", length = 500)
    private String descripcion;

    @Column(name = "CREADO_EN", nullable = false)
    @Builder.Default
    private LocalDateTime creadoEn = LocalDateTime.now();
}
