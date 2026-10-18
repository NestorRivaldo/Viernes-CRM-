package com.viernes.actividades.domain;

import java.time.LocalDateTime;

public class Actividad {
    private Long id;
    private String idUsuario;
    private Long idCliente;
    private Long idLead;
    private Long idOportunidad;
    private TipoActividad tipo;
    private String titulo;
    private String descripcion;
    private LocalDateTime fecha;
    private EstadoActividad estado;
    private LocalDateTime creadoEn;

    public Actividad() {
    }

    public Actividad(Long id, String idUsuario, Long idCliente, Long idLead, Long idOportunidad,
                     TipoActividad tipo, String titulo, String descripcion, LocalDateTime fecha,
                     EstadoActividad estado, LocalDateTime creadoEn) {
        this.id = id;
        this.idUsuario = idUsuario;
        this.idCliente = idCliente;
        this.idLead = idLead;
        this.idOportunidad = idOportunidad;
        this.tipo = tipo;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.fecha = fecha;
        this.estado = estado;
        this.creadoEn = creadoEn;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(String idUsuario) {
        this.idUsuario = idUsuario;
    }

    public Long getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(Long idCliente) {
        this.idCliente = idCliente;
    }

    public Long getIdLead() {
        return idLead;
    }

    public void setIdLead(Long idLead) {
        this.idLead = idLead;
    }

    public Long getIdOportunidad() {
        return idOportunidad;
    }

    public void setIdOportunidad(Long idOportunidad) {
        this.idOportunidad = idOportunidad;
    }

    public TipoActividad getTipo() {
        return tipo;
    }

    public void setTipo(TipoActividad tipo) {
        this.tipo = tipo;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public EstadoActividad getEstado() {
        return estado;
    }

    public void setEstado(EstadoActividad estado) {
        this.estado = estado;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(LocalDateTime creadoEn) {
        this.creadoEn = creadoEn;
    }
}
