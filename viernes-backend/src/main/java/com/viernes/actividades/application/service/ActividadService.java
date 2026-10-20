package com.viernes.actividades.application.service;

import com.viernes.actividades.application.dto.ActividadRequest;
import com.viernes.actividades.application.dto.ActividadResponse;
import com.viernes.actividades.application.dto.ParticipanteResponse;
import com.viernes.actividades.application.port.in.ManageActividadUseCase;
import com.viernes.actividades.application.port.out.ActividadParticipantePersistencePort;
import com.viernes.actividades.application.port.out.ActividadPersistencePort;
import com.viernes.actividades.application.port.out.ContactoQueryPort;
import com.viernes.actividades.application.port.out.RelacionValidationPort;
import com.viernes.actividades.domain.Actividad;
import com.viernes.actividades.domain.ActividadParticipante;
import com.viernes.actividades.domain.EstadoActividad;
import com.viernes.clientes.application.dto.ContactoResponse;
import com.viernes.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ActividadService implements ManageActividadUseCase {

    private final ActividadPersistencePort persistencePort;
    private final RelacionValidationPort validationPort;
    private final ActividadParticipantePersistencePort participantePort;
    private final ContactoQueryPort contactoQueryPort;

    public ActividadService(ActividadPersistencePort persistencePort, RelacionValidationPort validationPort,
                            ActividadParticipantePersistencePort participantePort, ContactoQueryPort contactoQueryPort) {
        this.persistencePort = persistencePort;
        this.validationPort = validationPort;
        this.participantePort = participantePort;
        this.contactoQueryPort = contactoQueryPort;
    }

    @Override
    public ActividadResponse crear(ActividadRequest request) {
        validarRelaciones(request);

        Actividad actividad = new Actividad();
        actividad.setIdUsuario(request.idUsuario());
        actividad.setIdCliente(request.idCliente());
        actividad.setIdLead(request.idLead());
        actividad.setIdOportunidad(request.idOportunidad());
        actividad.setTipo(request.tipo());
        actividad.setTitulo(request.titulo());
        actividad.setDescripcion(request.descripcion());
        actividad.setFecha(request.fecha());
        
        actividad.setEstado(request.estado() != null ? request.estado() : EstadoActividad.PENDIENTE);
        actividad.setCreadoEn(LocalDateTime.now());

        Actividad saved = persistencePort.save(actividad);
        return toResponse(saved);
    }

    @Override
    public ActividadResponse actualizar(Long id, ActividadRequest request) {
        validarRelaciones(request);

        Actividad actividad = persistencePort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Actividad no encontrada con ID: " + id));

        actividad.setIdUsuario(request.idUsuario());
        actividad.setIdCliente(request.idCliente());
        actividad.setIdLead(request.idLead());
        actividad.setIdOportunidad(request.idOportunidad());
        actividad.setTipo(request.tipo());
        actividad.setTitulo(request.titulo());
        actividad.setDescripcion(request.descripcion());
        actividad.setFecha(request.fecha());
        
        if (request.estado() != null) {
            actividad.setEstado(request.estado());
        }

        Actividad updated = persistencePort.save(actividad);
        return toResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public ActividadResponse obtener(Long id) {
        Actividad actividad = persistencePort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Actividad no encontrada con ID: " + id));
        return toResponse(actividad);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActividadResponse> listarTodos() {
        return persistencePort.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActividadResponse> listarVencidas() {
        return persistencePort.findVencidas(EstadoActividad.PENDIENTE, LocalDateTime.now())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActividadResponse> filtrarPorEstado(EstadoActividad estado) {
        return persistencePort.findByEstado(estado).stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActividadResponse> filtrarPorCliente(Long idCliente) {
        return persistencePort.findByIdCliente(idCliente).stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActividadResponse> filtrarPorLead(Long idLead) {
        return persistencePort.findByIdLead(idLead).stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActividadResponse> filtrarPorOportunidad(Long idOportunidad) {
        return persistencePort.findByIdOportunidad(idOportunidad).stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActividadResponse> filtrarPorUsuario(String idUsuario) {
        return persistencePort.findByIdUsuario(idUsuario).stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public ActividadResponse cambiarEstado(Long id, EstadoActividad estado) {
        Actividad actividad = persistencePort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Actividad no encontrada con ID: " + id));
        
        actividad.setEstado(estado);
        Actividad updated = persistencePort.save(actividad);
        return toResponse(updated);
    }

    @Override
    public void eliminar(Long id) {
        if (persistencePort.findById(id).isEmpty()) {
            throw new ResourceNotFoundException("Actividad no encontrada con ID: " + id);
        }
        participantePort.deleteByIdActividad(id);
        persistencePort.deleteById(id);
    }

    @Override
    public ParticipanteResponse agregarParticipante(Long actividadId, Long contactoId) {
        if (persistencePort.findById(actividadId).isEmpty()) {
            throw new ResourceNotFoundException("Actividad no encontrada con ID: " + actividadId);
        }
        ContactoResponse contacto = contactoQueryPort.buscarPorId(contactoId)
                .orElseThrow(() -> new ResourceNotFoundException("Contacto no encontrado con ID: " + contactoId));
        if (participantePort.existsByIdActividadAndIdContacto(actividadId, contactoId)) {
            throw new IllegalArgumentException("El contacto ya es participante de la actividad");
        }

        ActividadParticipante participante = new ActividadParticipante();
        participante.setIdActividad(actividadId);
        participante.setIdContacto(contactoId);
        participante.setCreadoEn(LocalDateTime.now());

        ActividadParticipante saved = participantePort.save(participante);
        return toParticipanteResponse(saved, contacto);
    }

    @Override
    public void quitarParticipante(Long actividadId, Long contactoId) {
        ActividadParticipante participante = participantePort
                .findByIdActividadAndIdContacto(actividadId, contactoId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El contacto " + contactoId + " no es participante de la actividad " + actividadId));
        participantePort.delete(participante);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipanteResponse> listarParticipantes(Long actividadId) {
        if (persistencePort.findById(actividadId).isEmpty()) {
            throw new ResourceNotFoundException("Actividad no encontrada con ID: " + actividadId);
        }
        return participantePort.findByIdActividad(actividadId).stream()
                .map(p -> toParticipanteResponse(p, contactoQueryPort.buscarPorId(p.getIdContacto()).orElse(null)))
                .collect(Collectors.toList());
    }

    private void validarRelaciones(ActividadRequest request) {
        if (request.idCliente() != null && !validationPort.existeCliente(request.idCliente())) {
            throw new ResourceNotFoundException("Cliente no encontrado con ID: " + request.idCliente());
        }
        if (request.idLead() != null && !validationPort.existeLead(request.idLead())) {
            throw new ResourceNotFoundException("Lead no encontrado con ID: " + request.idLead());
        }
        if (request.idOportunidad() != null && !validationPort.existeOportunidad(request.idOportunidad())) {
            throw new ResourceNotFoundException("Oportunidad no encontrada con ID: " + request.idOportunidad());
        }
        if (request.idUsuario() != null && !validationPort.existeUsuario(request.idUsuario())) {
            throw new ResourceNotFoundException("Usuario no encontrado con ID: " + request.idUsuario());
        }
    }

    private ParticipanteResponse toParticipanteResponse(ActividadParticipante participante, ContactoResponse contacto) {
        String nombre = null;
        String correo = null;
        if (contacto != null) {
            nombre = contacto.apellido() != null && !contacto.apellido().isBlank()
                    ? contacto.nombre() + " " + contacto.apellido()
                    : contacto.nombre();
            correo = contacto.correo();
        }
        return new ParticipanteResponse(
                participante.getId(),
                participante.getIdActividad(),
                participante.getIdContacto(),
                nombre,
                correo,
                participante.getCreadoEn()
        );
    }

    private ActividadResponse toResponse(Actividad actividad) {
        boolean esVencida = actividad.getEstado() == EstadoActividad.PENDIENTE && 
                            actividad.getFecha().isBefore(LocalDateTime.now());

        return new ActividadResponse(
                actividad.getId(),
                actividad.getIdUsuario(),
                actividad.getIdCliente(),
                actividad.getIdLead(),
                actividad.getIdOportunidad(),
                actividad.getTipo(),
                actividad.getTitulo(),
                actividad.getDescripcion(),
                actividad.getFecha(),
                actividad.getEstado(),
                actividad.getCreadoEn(),
                esVencida
        );
    }
}
