package com.viernes.actividades.application.service;

import com.viernes.actividades.application.dto.ActividadRequest;
import com.viernes.actividades.application.dto.ActividadResponse;
import com.viernes.actividades.application.dto.ParticipanteResponse;
import com.viernes.actividades.application.port.in.ManageActividadUseCase;
import com.viernes.actividades.application.port.out.ActividadEtiquetaPersistencePort;
import com.viernes.actividades.application.port.out.ActividadParticipantePersistencePort;
import com.viernes.actividades.application.port.out.ActividadPersistencePort;
import com.viernes.actividades.application.port.out.ContactoQueryPort;
import com.viernes.actividades.application.port.out.RelacionValidationPort;
import com.viernes.actividades.domain.Actividad;
import com.viernes.actividades.domain.ActividadEtiqueta;
import com.viernes.actividades.domain.ActividadParticipante;
import com.viernes.actividades.domain.EstadoActividad;
import com.viernes.clientes.application.dto.ContactoResponse;
import com.viernes.etiquetas.application.dto.EtiquetaResponse;
import com.viernes.etiquetas.application.port.out.EtiquetaPersistencePort;
import com.viernes.etiquetas.domain.Etiqueta;
import com.viernes.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class ActividadService implements ManageActividadUseCase {

    private static final int MAX_ETIQUETAS = 8;

    private final ActividadPersistencePort persistencePort;
    private final RelacionValidationPort validationPort;
    private final ActividadParticipantePersistencePort participantePort;
    private final ContactoQueryPort contactoQueryPort;
    private final ActividadEtiquetaPersistencePort actividadEtiquetaPort;
    private final EtiquetaPersistencePort etiquetaPort;

    public ActividadService(ActividadPersistencePort persistencePort,
                            RelacionValidationPort validationPort,
                            ActividadParticipantePersistencePort participantePort,
                            ContactoQueryPort contactoQueryPort,
                            ActividadEtiquetaPersistencePort actividadEtiquetaPort,
                            EtiquetaPersistencePort etiquetaPort) {
        this.persistencePort = persistencePort;
        this.validationPort = validationPort;
        this.participantePort = participantePort;
        this.contactoQueryPort = contactoQueryPort;
        this.actividadEtiquetaPort = actividadEtiquetaPort;
        this.etiquetaPort = etiquetaPort;
    }

    @Override
    public ActividadResponse crear(ActividadRequest request) {
        validarRelaciones(request);
        List<Long> etiquetaIds = etiquetaIdsValidos(request.etiquetaIds());

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
        sincronizarEtiquetas(saved.getId(), etiquetaIds);
        return toResponse(saved, cargarEtiquetas(saved.getId()));
    }

    @Override
    public ActividadResponse actualizar(Long id, ActividadRequest request) {
        validarRelaciones(request);

        Actividad actividad = persistencePort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Actividad no encontrada con ID: " + id));

        List<Long> etiquetaIds = request.etiquetaIds() == null
                ? null
                : etiquetaIdsValidos(request.etiquetaIds());

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
        if (etiquetaIds != null) {
            sincronizarEtiquetas(updated.getId(), etiquetaIds);
        }
        return toResponse(updated, cargarEtiquetas(updated.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public ActividadResponse obtener(Long id) {
        Actividad actividad = persistencePort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Actividad no encontrada con ID: " + id));
        return toResponse(actividad, cargarEtiquetas(actividad.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActividadResponse> listarTodos() {
        return toResponses(persistencePort.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActividadResponse> listarVencidas() {
        return toResponses(persistencePort.findVencidas(EstadoActividad.PENDIENTE, LocalDateTime.now()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActividadResponse> filtrarPorEstado(EstadoActividad estado) {
        return toResponses(persistencePort.findByEstado(estado));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActividadResponse> filtrarPorCliente(Long idCliente) {
        return toResponses(persistencePort.findByIdCliente(idCliente));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActividadResponse> filtrarPorLead(Long idLead) {
        return toResponses(persistencePort.findByIdLead(idLead));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActividadResponse> filtrarPorOportunidad(Long idOportunidad) {
        return toResponses(persistencePort.findByIdOportunidad(idOportunidad));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActividadResponse> filtrarPorEtiqueta(Long idEtiqueta) {
        if (etiquetaPort.findById(idEtiqueta).isEmpty()) {
            throw new ResourceNotFoundException("Etiqueta no encontrada con ID: " + idEtiqueta);
        }
        return toResponses(persistencePort.findByEtiquetaId(idEtiqueta));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActividadResponse> filtrarPorUsuario(String idUsuario) {
        return toResponses(persistencePort.findByIdUsuario(idUsuario));
    }

    @Override
    public ActividadResponse cambiarEstado(Long id, EstadoActividad estado) {
        Actividad actividad = persistencePort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Actividad no encontrada con ID: " + id));

        actividad.setEstado(estado);
        Actividad updated = persistencePort.save(actividad);
        return toResponse(updated, cargarEtiquetas(updated.getId()));
    }

    @Override
    public void eliminar(Long id) {
        if (persistencePort.findById(id).isEmpty()) {
            throw new ResourceNotFoundException("Actividad no encontrada con ID: " + id);
        }
        actividadEtiquetaPort.deleteByIdActividad(id);
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

    private List<Long> etiquetaIdsValidos(List<Long> etiquetaIds) {
        if (etiquetaIds == null || etiquetaIds.isEmpty()) {
            return List.of();
        }

        List<Long> limpios = etiquetaIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (limpios.size() > MAX_ETIQUETAS) {
            throw new IllegalArgumentException("Una actividad puede tener como máximo " + MAX_ETIQUETAS + " etiquetas");
        }

        if (limpios.isEmpty()) {
            return limpios;
        }

        List<Etiqueta> encontradas = etiquetaPort.findAllById(limpios);
        Set<Long> existentes = encontradas.stream()
                .map(Etiqueta::getId)
                .collect(Collectors.toSet());

        List<Long> faltantes = limpios.stream()
                .filter(id -> !existentes.contains(id))
                .toList();

        if (!faltantes.isEmpty()) {
            throw new ResourceNotFoundException("Etiquetas no encontradas: " + faltantes);
        }

        return limpios;
    }

    private void sincronizarEtiquetas(Long idActividad, List<Long> etiquetaIds) {
        if (etiquetaIds == null) {
            return;
        }
        actividadEtiquetaPort.reemplazarEtiquetas(idActividad, etiquetaIds);
    }

    private List<EtiquetaResponse> cargarEtiquetas(Long idActividad) {
        List<Long> ids = actividadEtiquetaPort.findByIdActividad(idActividad).stream()
                .map(ActividadEtiqueta::getIdEtiqueta)
                .toList();
        return cargarEtiquetasPorIds(ids);
    }

    private List<EtiquetaResponse> cargarEtiquetasPorIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        Map<Long, EtiquetaResponse> etiquetasPorId = etiquetaPort.findAllById(ids).stream()
                .map(this::toEtiquetaResponse)
                .collect(Collectors.toMap(EtiquetaResponse::id, e -> e));

        return ids.stream()
                .map(etiquetasPorId::get)
                .filter(Objects::nonNull)
                .toList();
    }

    private List<ActividadResponse> toResponses(List<Actividad> actividades) {
        if (actividades == null || actividades.isEmpty()) {
            return List.of();
        }

        List<Long> idsActividad = actividades.stream()
                .map(Actividad::getId)
                .filter(Objects::nonNull)
                .toList();

        Map<Long, List<Long>> etiquetasPorActividad = new HashMap<>();
        List<ActividadEtiqueta> vinculos = actividadEtiquetaPort.findByIdActividadIn(idsActividad);
        if (vinculos != null) {
            for (ActividadEtiqueta vinculo : vinculos) {
                etiquetasPorActividad
                        .computeIfAbsent(vinculo.getIdActividad(), k -> new ArrayList<>())
                        .add(vinculo.getIdEtiqueta());
            }
        }

        Set<Long> idsEtiqueta = new HashSet<>();
        etiquetasPorActividad.values().forEach(idsEtiqueta::addAll);

        Map<Long, EtiquetaResponse> etiquetasPorId = etiquetaPort.findAllById(idsEtiqueta).stream()
                .map(this::toEtiquetaResponse)
                .collect(Collectors.toMap(EtiquetaResponse::id, e -> e));

        return actividades.stream()
                .map(actividad -> {
                    List<EtiquetaResponse> etiquetas = etiquetasPorActividad
                            .getOrDefault(actividad.getId(), List.of())
                            .stream()
                            .map(etiquetasPorId::get)
                            .filter(Objects::nonNull)
                            .toList();
                    return toResponse(actividad, etiquetas);
                })
                .toList();
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

    private EtiquetaResponse toEtiquetaResponse(Etiqueta etiqueta) {
        return new EtiquetaResponse(
                etiqueta.getId(),
                etiqueta.getNombre(),
                etiqueta.getColor(),
                etiqueta.getDescripcion(),
                etiqueta.getActiva(),
                etiqueta.getCreadoEn()
        );
    }

    private ActividadResponse toResponse(Actividad actividad, List<EtiquetaResponse> etiquetas) {
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
                esVencida,
                etiquetas != null ? etiquetas : List.of()
        );
    }
}
