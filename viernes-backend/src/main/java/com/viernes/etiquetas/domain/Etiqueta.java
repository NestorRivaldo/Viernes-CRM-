package com.viernes.etiquetas.domain;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Etiqueta {
    private Long id;
    private String nombre;
    private String nombreNormalizado;
    private String color;
    private String descripcion;
    private Boolean activa;
    private LocalDateTime creadoEn;
}
