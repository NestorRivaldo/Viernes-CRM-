package com.viernes.equipos.domain;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EquipoMiembro {
    private Long id;
    private Long equipoId;
    private Long usuarioId;
    private LocalDateTime creadoEn;
}
