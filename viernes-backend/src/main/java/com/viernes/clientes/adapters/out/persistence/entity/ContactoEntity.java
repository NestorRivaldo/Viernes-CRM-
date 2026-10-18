package com.viernes.clientes.adapters.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "CRM_CONTACTOS")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ContactoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    // Solo lectura: la relación ManyToOne escribe esta columna vía cliente
    @Column(name = "ID_CLIENTE", nullable = false, insertable = false, updatable = false)
    private Long idCliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_CLIENTE", nullable = false)
    private ClienteEntity cliente;

    @Column(name = "NOMBRE", nullable = false, length = 100)
    private String nombre;

    @Column(name = "APELLIDO", length = 100)
    private String apellido;

    @Column(name = "CARGO", length = 100)
    private String cargo;

    @Column(name = "CORREO", length = 150)
    private String correo;

    @Column(name = "TELEFONO", length = 30)
    private String telefono;

    @Column(name = "PRINCIPAL")
    @Builder.Default
    private Boolean principal = false;

    @Column(name = "CREADO_EN", nullable = false)
    @Builder.Default
    private LocalDateTime creadoEn = LocalDateTime.now();
}
