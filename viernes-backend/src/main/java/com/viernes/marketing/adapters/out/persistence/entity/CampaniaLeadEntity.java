package com.viernes.marketing.adapters.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "CRM_CAMPANIA_LEADS", uniqueConstraints =
        @UniqueConstraint(name = "UK_CAMPANIA_LEAD", columnNames = {"CAMPANIA_ID", "LEAD_ID"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CampaniaLeadEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "CAMPANIA_ID", nullable = false)
    private Long campaniaId;

    @Column(name = "LEAD_ID", nullable = false)
    private Long leadId;

    @Column(name = "CREADO_EN", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime creadoEn = LocalDateTime.now();
}
