package com.viernes.marketing.domain;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CampaniaLead {
    private Long id;
    private Long campaniaId;
    private Long leadId;
    private LocalDateTime creadoEn;
}
