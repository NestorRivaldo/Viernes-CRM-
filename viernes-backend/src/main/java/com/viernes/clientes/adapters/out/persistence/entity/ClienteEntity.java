package com.viernes.clientes.adapters.out.persistence.entity;

import com.viernes.clientes.domain.EstadoCliente;
import com.viernes.clientes.domain.TipoDocumento;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "CRM_CLIENTES",
        uniqueConstraints = @UniqueConstraint(
                name = "UK_CRM_CLIENTES_DOCUMENTO",
                columnNames = {"TIPO_DOCUMENTO", "NUMERO_DOCUMENTO"}
        )
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ClienteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "NOMBRE", nullable = false, length = 150)
    private String nombre;

    @Column(name = "EMPRESA", length = 150)
    private String empresa;

    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO_DOCUMENTO", length = 10)
    private TipoDocumento tipoDocumento;

    @Column(name = "NUMERO_DOCUMENTO", length = 20)
    private String numeroDocumento;

    @Column(name = "CORREO", length = 150)
    private String correo;

    @Column(name = "TELEFONO", length = 30)
    private String telefono;

    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO", nullable = false, length = 20)
    @Builder.Default
    private EstadoCliente estado = EstadoCliente.ACTIVO;

    @Column(name = "RESPONSABLE", length = 100)
    private String responsable;

    @Column(name = "CREADO_EN", nullable = false)
    @Builder.Default
    private LocalDateTime creadoEn = LocalDateTime.now();

    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ContactoEntity> contactos = new ArrayList<>();
}
