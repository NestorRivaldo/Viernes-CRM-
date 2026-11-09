package com.viernes.actividades.adapters.out.persistence.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "CRM_ACTIVIDAD_ETIQUETAS", uniqueConstraints = {
        @UniqueConstraint(name = "uk_actividad_etiqueta", columnNames = {"id_actividad", "id_etiqueta"})
})
public class ActividadEtiquetaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_actividad_etiqueta")
    private Long id;

    @Column(name = "id_actividad", nullable = false)
    private Long idActividad;

    @Column(name = "id_etiqueta", nullable = false)
    private Long idEtiqueta;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @Column(name = "orden")
    private Integer orden;

    public ActividadEtiquetaEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getIdActividad() {
        return idActividad;
    }

    public void setIdActividad(Long idActividad) {
        this.idActividad = idActividad;
    }

    public Long getIdEtiqueta() {
        return idEtiqueta;
    }

    public void setIdEtiqueta(Long idEtiqueta) {
        this.idEtiqueta = idEtiqueta;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(LocalDateTime creadoEn) {
        this.creadoEn = creadoEn;
    }

    public Integer getOrden() {
        return orden;
    }

    public void setOrden(Integer orden) {
        this.orden = orden;
    }
}
