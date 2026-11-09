package com.viernes.actividades.domain;

import java.time.LocalDateTime;

public class ActividadEtiqueta {
    private Long id;
    private Long idActividad;
    private Long idEtiqueta;
    private LocalDateTime creadoEn;
    private Integer orden;

    public ActividadEtiqueta() {
    }

    public ActividadEtiqueta(Long id, Long idActividad, Long idEtiqueta, LocalDateTime creadoEn) {
        this(id, idActividad, idEtiqueta, creadoEn, null);
    }

    public ActividadEtiqueta(Long id, Long idActividad, Long idEtiqueta, LocalDateTime creadoEn, Integer orden) {
        this.id = id;
        this.idActividad = idActividad;
        this.idEtiqueta = idEtiqueta;
        this.creadoEn = creadoEn;
        this.orden = orden;
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
