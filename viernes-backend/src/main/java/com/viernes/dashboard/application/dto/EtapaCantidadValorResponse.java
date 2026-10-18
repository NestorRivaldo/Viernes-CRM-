package com.viernes.dashboard.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EtapaCantidadValorResponse {
    private String etapa;
    private long cantidad;
    private BigDecimal valorTotal;
}
