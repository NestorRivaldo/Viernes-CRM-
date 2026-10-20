package com.viernes.equipos.application.service;

import com.viernes.equipos.application.dto.EquipoRequest;
import com.viernes.equipos.application.dto.EquipoResponse;
import com.viernes.equipos.application.dto.MiembroResponse;
import com.viernes.equipos.application.port.in.ManageEquipoUseCase;
import com.viernes.equipos.application.port.out.EquipoMiembroPersistencePort;
import com.viernes.equipos.application.port.out.EquipoPersistencePort;
import com.viernes.equipos.application.port.out.UsuarioValidationPort;
import com.viernes.equipos.domain.Equipo;
import com.viernes.equipos.domain.EquipoMiembro;
import com.viernes.equipos.domain.EstadoEquipo;
import com.viernes.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EquipoService implements ManageEquipoUseCase {

    private final EquipoPersistencePort persistencePort;
    private final EquipoMiembroPersistencePort miembroPersistencePort;
    private final UsuarioValidationPort usuarioValidationPort;

    @Override
    @Transactional(readOnly = true)
    public List<EquipoResponse> listar() {
        return persistencePort.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipoResponse> listarPorEstado(EstadoEquipo estado) {
        return persistencePort.findByEstado(estado).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EquipoResponse obtener(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    @Transactional
    public EquipoResponse crear(EquipoRequest request) {
        if (persistencePort.existsByNombre(request.nombre())) {
            throw new IllegalArgumentException("Ya existe un equipo con el nombre: " + request.nombre());
        }
        Equipo equipo = Equipo.builder()
                .nombre(request.nombre())
                .descripcion(request.descripcion())
                .estado(EstadoEquipo.ACTIVO)
                .creadoEn(LocalDateTime.now())
                .build();
        return toResponse(persistencePort.save(equipo));
    }

    @Override
    @Transactional
    public EquipoResponse actualizar(Long id, EquipoRequest request) {
        Equipo equipo = findOrThrow(id);
        if (!equipo.getNombre().equals(request.nombre()) && persistencePort.existsByNombre(request.nombre())) {
            throw new IllegalArgumentException("Ya existe un equipo con el nombre: " + request.nombre());
        }
        equipo.setNombre(request.nombre());
        equipo.setDescripcion(request.descripcion());
        return toResponse(persistencePort.save(equipo));
    }

    @Override
    @Transactional
    public void activar(Long id) {
        Equipo equipo = findOrThrow(id);
        equipo.setEstado(EstadoEquipo.ACTIVO);
        persistencePort.save(equipo);
    }

    @Override
    @Transactional
    public void desactivar(Long id) {
        Equipo equipo = findOrThrow(id);
        equipo.setEstado(EstadoEquipo.INACTIVO);
        persistencePort.save(equipo);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        findOrThrow(id);
        miembroPersistencePort.deleteByEquipoId(id);
        persistencePort.deleteById(id);
    }

    @Override
    @Transactional
    public MiembroResponse agregarMiembro(Long equipoId, Long usuarioId) {
        findOrThrow(equipoId);
        if (!usuarioValidationPort.existeUsuario(usuarioId)) {
            throw new ResourceNotFoundException("Usuario no encontrado: " + usuarioId);
        }
        if (miembroPersistencePort.existsByEquipoIdAndUsuarioId(equipoId, usuarioId)) {
            throw new IllegalArgumentException("El usuario ya es miembro del equipo: " + usuarioId);
        }
        EquipoMiembro miembro = EquipoMiembro.builder()
                .equipoId(equipoId)
                .usuarioId(usuarioId)
                .creadoEn(LocalDateTime.now())
                .build();
        return toMiembroResponse(miembroPersistencePort.save(miembro));
    }

    @Override
    @Transactional
    public void quitarMiembro(Long equipoId, Long usuarioId) {
        findOrThrow(equipoId);
        if (!miembroPersistencePort.existsByEquipoIdAndUsuarioId(equipoId, usuarioId)) {
            throw new ResourceNotFoundException(
                    "El usuario " + usuarioId + " no es miembro del equipo: " + equipoId);
        }
        miembroPersistencePort.deleteByEquipoIdAndUsuarioId(equipoId, usuarioId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MiembroResponse> listarMiembros(Long equipoId) {
        findOrThrow(equipoId);
        return miembroPersistencePort.findByEquipoId(equipoId).stream()
                .map(this::toMiembroResponse)
                .toList();
    }

    private Equipo findOrThrow(Long id) {
        return persistencePort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Equipo no encontrado: " + id));
    }

    private EquipoResponse toResponse(Equipo e) {
        long totalMiembros = miembroPersistencePort.countByEquipoId(e.getId());
        return new EquipoResponse(e.getId(), e.getNombre(), e.getDescripcion(),
                EstadoEquipo.ACTIVO.equals(e.getEstado()), e.getCreadoEn(), totalMiembros);
    }

    private MiembroResponse toMiembroResponse(EquipoMiembro m) {
        return usuarioValidationPort.buscarUsuario(m.getUsuarioId())
                .map(u -> new MiembroResponse(m.getUsuarioId(),
                        (u.nombre() + " " + u.apellido()).trim(), u.correo(), m.getCreadoEn()))
                .orElseGet(() -> new MiembroResponse(m.getUsuarioId(), null, null, m.getCreadoEn()));
    }
}
