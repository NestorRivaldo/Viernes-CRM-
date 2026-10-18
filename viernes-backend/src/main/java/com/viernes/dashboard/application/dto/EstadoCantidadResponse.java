package com.viernes.dashboard.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EstadoCantidadResponse {
    private String estado;
    private long cantidad;
}
