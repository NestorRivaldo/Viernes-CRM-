package com.viernes.leads.domain;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Lead {
    private Long id;
    private String nombre;
    private String empresa;
    private String correo;
    private String telefono;
    private String fuente;
    private EstadoLead estado;
    private String responsable;
    private LocalDate fecha;
    private LocalDateTime creadoEn;
}
