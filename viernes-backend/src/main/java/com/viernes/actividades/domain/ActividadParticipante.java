package com.viernes.actividades.domain;

import java.time.LocalDateTime;

public class ActividadParticipante {
    private Long id;
    private Long idActividad;
    private Long idContacto;
    private LocalDateTime creadoEn;

    public ActividadParticipante() {
    }

    public ActividadParticipante(Long id, Long idActividad, Long idContacto, LocalDateTime creadoEn) {
        this.id = id;
        this.idActividad = idActividad;
        this.idContacto = idContacto;
        this.creadoEn = creadoEn;
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

    public Long getIdContacto() {
        return idContacto;
    }

    public void setIdContacto(Long idContacto) {
        this.idContacto = idContacto;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(LocalDateTime creadoEn) {
        this.creadoEn = creadoEn;
    }
}
