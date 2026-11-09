package com.viernes.oportunidades.domain;

import lombok.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Oportunidad {
    private Long id;
    private String nombre;
    private String clienteNombre;
    private BigDecimal valor;
    private Integer probabilidad;
    private EtapaOportunidad etapa;
    private String responsable;
    private LocalDate fechaCierre;
    private String descripcion;
    private LocalDateTime creadoEn;

    public BigDecimal getValorPonderado() {
        if (valor == null || probabilidad == null) return BigDecimal.ZERO;
        return valor.multiply(BigDecimal.valueOf(probabilidad))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}
