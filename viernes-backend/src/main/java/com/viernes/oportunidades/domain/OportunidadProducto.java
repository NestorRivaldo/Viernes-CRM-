package com.viernes.oportunidades.domain;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OportunidadProducto {
    private Long id;
    private Long idOportunidad;
    private Long idProducto;
    private Integer cantidad;
    private LocalDateTime creadoEn;
}
