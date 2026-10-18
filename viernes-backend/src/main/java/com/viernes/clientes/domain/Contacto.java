package com.viernes.clientes.domain;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Contacto {
    private Long id;
    private Long idCliente;
    private String nombre;
    private String apellido;
    private String cargo;
    private String correo;
    private String telefono;
    private Boolean principal;
    private LocalDateTime creadoEn;
}
