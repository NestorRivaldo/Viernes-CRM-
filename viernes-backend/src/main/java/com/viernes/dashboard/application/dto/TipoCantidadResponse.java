package com.viernes.dashboard.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TipoCantidadResponse {
    private String tipo;
    private long cantidad;
}
