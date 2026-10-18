package com.viernes.usuarios.adapters.out.persistence;

import com.viernes.usuarios.adapters.out.persistence.entity.UsuarioEntity;
import com.viernes.usuarios.adapters.out.persistence.repository.UsuarioSpringDataRepository;
import com.viernes.usuarios.application.port.out.UsuarioPersistencePort;
import com.viernes.usuarios.domain.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UsuarioPersistenceAdapter implements UsuarioPersistencePort {

    private final UsuarioSpringDataRepository repository;

    @Override
    public List<Usuario> findAll() {
        return repository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Usuario> findById(Long id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Usuario> findByCorreo(String correo) {
        return repository.findByCorreo(correo).map(this::toDomain);
    }

    @Override
    public boolean existsByCorreo(String correo) {
        return repository.existsByCorreo(correo);
    }

    @Override
    public boolean existsById(Long id) {
        return repository.existsById(id);
    }

    @Override
    public Usuario save(Usuario usuario) {
        return toDomain(repository.save(toEntity(usuario)));
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    private Usuario toDomain(UsuarioEntity e) {
        return Usuario.builder()
                .id(e.getId())
                .nombre(e.getNombre())
                .apellido(e.getApellido())
                .correo(e.getCorreo())
                .passwordHash(e.getPasswordHash())
                .rol(e.getRol())
                .estado(e.getEstado())
                .creadoEn(e.getCreadoEn())
                .actualizadoEn(e.getActualizadoEn())
                .build();
    }

    private UsuarioEntity toEntity(Usuario u) {
        return UsuarioEntity.builder()
                .id(u.getId())
                .nombre(u.getNombre())
                .apellido(u.getApellido())
                .correo(u.getCorreo())
                .passwordHash(u.getPasswordHash())
                .rol(u.getRol())
                .estado(u.getEstado())
                .creadoEn(u.getCreadoEn())
                .actualizadoEn(u.getActualizadoEn())
                .build();
    }
}
