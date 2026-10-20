package com.viernes.equipos.domain;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Equipo {
    private Long id;
    private String nombre;
    private String descripcion;
    private EstadoEquipo estado;
    private LocalDateTime creadoEn;
}
