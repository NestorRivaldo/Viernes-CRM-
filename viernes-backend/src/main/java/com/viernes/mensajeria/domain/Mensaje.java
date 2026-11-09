package com.viernes.mensajeria.domain;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mensaje {
    private Long id;
    private String usuarioCorreo;
    private String usuarioNombre;
    private String contenido;
    private LocalDateTime creadoEn;
}
