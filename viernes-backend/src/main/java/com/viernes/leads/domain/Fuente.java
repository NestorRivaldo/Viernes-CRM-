package com.viernes.leads.domain;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Fuente {
    private Long id;
    private String codigo;
    private String nombre;
    private String descripcion;
    private Boolean activa;
    private LocalDateTime creadoEn;
}
