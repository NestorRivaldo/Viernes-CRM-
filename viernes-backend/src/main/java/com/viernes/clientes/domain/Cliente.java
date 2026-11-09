package com.viernes.clientes.domain;

import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Cliente {
    private Long id;
    private String nombre;
    private String empresa;
    private TipoDocumento tipoDocumento;
    private String numeroDocumento;
    private String correo;
    private String telefono;
    private EstadoCliente estado;
    private String responsable;
    private LocalDateTime creadoEn;
    
    @Builder.Default
    private List<Contacto> contactos = new ArrayList<>();
}
