package com.viernes.usuarios.application.service;

import com.viernes.usuarios.UsuarioResumen;
import com.viernes.usuarios.UsuariosApi;
import com.viernes.usuarios.application.dto.UsuarioRequest;
import com.viernes.usuarios.application.dto.UsuarioResponse;
import com.viernes.usuarios.application.port.in.ManageUsuarioUseCase;
import com.viernes.usuarios.application.port.in.UsuarioConsultaPort;
import com.viernes.usuarios.application.port.out.UsuarioPersistencePort;
import com.viernes.usuarios.domain.EstadoUsuario;
import com.viernes.usuarios.domain.Usuario;
import com.viernes.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UsuarioService implements ManageUsuarioUseCase, UsuarioConsultaPort, UsuariosApi {

    private final UsuarioPersistencePort persistencePort;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<UsuarioResponse> listar() {
        return persistencePort.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public UsuarioResponse obtener(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    public UsuarioResponse obtenerPorCorreo(String correo) {
        return toResponse(persistencePort.findByCorreo(correo)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con correo: " + correo)));
    }

    @Override
    @Transactional
    public UsuarioResponse crear(UsuarioRequest request) {
        if (request.password() == null || request.password().isBlank()) {
            throw new IllegalArgumentException("La contraseña es obligatoria para crear un usuario");
        }
        if (persistencePort.existsByCorreo(request.correo())) {
            throw new IllegalArgumentException("Ya existe un usuario con el correo: " + request.correo());
        }
        Usuario usuario = Usuario.builder()
                .nombre(request.nombre())
                .apellido(request.apellido())
                .correo(request.correo())
                .passwordHash(passwordEncoder.encode(request.password()))
                .rol(request.rol())
                .estado(EstadoUsuario.ACTIVO)
                .creadoEn(LocalDateTime.now())
                .build();
        return toResponse(persistencePort.save(usuario));
    }

    @Override
    @Transactional
    public UsuarioResponse actualizar(Long id, UsuarioRequest request) {
        Usuario usuario = findOrThrow(id);
        if (!usuario.getCorreo().equals(request.correo()) && persistencePort.existsByCorreo(request.correo())) {
            throw new IllegalArgumentException("Ya existe un usuario con el correo: " + request.correo());
        }
        usuario.setNombre(request.nombre());
        usuario.setApellido(request.apellido());
        usuario.setCorreo(request.correo());
        usuario.setRol(request.rol());
        if (request.password() != null && !request.password().isBlank()) {
            usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        usuario.setActualizadoEn(LocalDateTime.now());
        return toResponse(persistencePort.save(usuario));
    }

    @Override
    @Transactional
    public void activar(Long id) {
        Usuario usuario = findOrThrow(id);
        usuario.setEstado(EstadoUsuario.ACTIVO);
        usuario.setActualizadoEn(LocalDateTime.now());
        persistencePort.save(usuario);
    }

    @Override
    @Transactional
    public void desactivar(Long id) {
        Usuario usuario = findOrThrow(id);
        usuario.setEstado(EstadoUsuario.INACTIVO);
        usuario.setActualizadoEn(LocalDateTime.now());
        persistencePort.save(usuario);
    }

    private Usuario findOrThrow(Long id) {
        return persistencePort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + id));
    }

    private UsuarioResponse toResponse(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getNombre(), u.getApellido(),
                u.getCorreo(), u.getRol(), u.getEstado(), u.getCreadoEn());
    }

    @Override
    public Optional<UsuarioResumen> buscarPorCorreo(String correo) {
        return persistencePort.findByCorreo(correo).map(u -> new UsuarioResumen(
                u.getId(), u.getNombre(), u.getApellido(), u.getCorreo(),
                u.getPasswordHash(),
                u.getRol() != null ? u.getRol().name() : null,
                u.getEstado() != null ? u.getEstado().name() : null,
                u.getCreadoEn()));
    }

    @Override
    public boolean existePorId(Long id) {
        return persistencePort.existsById(id);
    }

    @Override
    public boolean existePorCorreo(String correo) {
        return persistencePort.existsByCorreo(correo);
    }
}
