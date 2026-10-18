package com.viernes.actividades.adapters.out.persistence.entity;

import com.viernes.actividades.domain.EstadoActividad;
import com.viernes.actividades.domain.TipoActividad;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "CRM_ACTIVIDADES")
public class ActividadEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_actividad")
    private Long id;

    @Column(name = "id_usuario")
    private String idUsuario;

    @Column(name = "id_cliente")
    private Long idCliente;

    @Column(name = "id_lead")
    private Long idLead;

    @Column(name = "id_oportunidad")
    private Long idOportunidad;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoActividad tipo;

    @Column(name = "titulo", nullable = false)
    private String titulo;

    @Lob
    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoActividad estado;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    public ActividadEntity() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getIdUsuario() { return idUsuario; }
    public void setIdUsuario(String idUsuario) { this.idUsuario = idUsuario; }
    public Long getIdCliente() { return idCliente; }
    public void setIdCliente(Long idCliente) { this.idCliente = idCliente; }
    public Long getIdLead() { return idLead; }
    public void setIdLead(Long idLead) { this.idLead = idLead; }
    public Long getIdOportunidad() { return idOportunidad; }
    public void setIdOportunidad(Long idOportunidad) { this.idOportunidad = idOportunidad; }
    public TipoActividad getTipo() { return tipo; }
    public void setTipo(TipoActividad tipo) { this.tipo = tipo; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public EstadoActividad getEstado() { return estado; }
    public void setEstado(EstadoActividad estado) { this.estado = estado; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
    public void setCreadoEn(LocalDateTime creadoEn) { this.creadoEn = creadoEn; }
}
