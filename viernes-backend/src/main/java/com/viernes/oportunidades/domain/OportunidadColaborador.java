package com.viernes.oportunidades.domain;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OportunidadColaborador {
    private Long id;
    private Long idOportunidad;
    private Long idUsuario;
    private LocalDateTime creadoEn;
}
