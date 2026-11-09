package com.viernes.oportunidades.application.dto;

import com.viernes.oportunidades.domain.EtapaOportunidad;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record OportunidadResponse(
    Long id,
    String nombre,
    String clienteNombre,
    BigDecimal valor,
    Integer probabilidad,
    BigDecimal valorPonderado,
    EtapaOportunidad etapa,
    String responsable,
    LocalDate fechaCierre,
    String descripcion,
    LocalDateTime creadoEn
) {}
