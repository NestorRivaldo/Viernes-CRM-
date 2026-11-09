package com.viernes.oportunidades.domain;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OportunidadContacto {
    private Long id;
    private Long idOportunidad;
    private Long idContacto;
    private String rol;
    private Boolean esDecisor;
    private LocalDateTime creadoEn;
}
