package com.viernes.usuarios.domain;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Usuario {
    private Long id;
    private String nombre;
    private String apellido;
    private String correo;
    private String passwordHash;
    private RolUsuario rol;
    private EstadoUsuario estado;
    private LocalDateTime creadoEn;
    private LocalDateTime actualizadoEn;
}
