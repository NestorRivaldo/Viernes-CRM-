package com.viernes.mensajeria.adapters.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "CRM_MENSAJES")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MensajeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "USUARIO_CORREO", nullable = false, length = 150)
    private String usuarioCorreo;

    @Column(name = "USUARIO_NOMBRE", nullable = false, length = 200)
    private String usuarioNombre;

    @Column(name = "CONTENIDO", nullable = false, length = 2000)
    private String contenido;

    @Column(name = "CREADO_EN", nullable = false)
    private LocalDateTime creadoEn;
}
