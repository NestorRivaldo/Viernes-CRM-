package com.viernes.actividades.adapters.out.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "CRM_ACTIVIDAD_PARTICIPANTES", uniqueConstraints = {
        @UniqueConstraint(name = "uk_actividad_contacto", columnNames = {"id_actividad", "id_contacto"})
})
public class ActividadParticipanteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_participante")
    private Long id;

    @Column(name = "id_actividad", nullable = false)
    private Long idActividad;

    // Referencia lógica a CRM_CONTACTOS (módulo clientes), sin foreign key física
    @Column(name = "id_contacto", nullable = false)
    private Long idContacto;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    public ActividadParticipanteEntity() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getIdActividad() { return idActividad; }
    public void setIdActividad(Long idActividad) { this.idActividad = idActividad; }
    public Long getIdContacto() { return idContacto; }
    public void setIdContacto(Long idContacto) { this.idContacto = idContacto; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
    public void setCreadoEn(LocalDateTime creadoEn) { this.creadoEn = creadoEn; }
}
