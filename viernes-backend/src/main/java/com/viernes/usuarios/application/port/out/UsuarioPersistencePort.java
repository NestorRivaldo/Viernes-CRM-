package com.viernes.usuarios.application.port.out;

import com.viernes.usuarios.domain.Usuario;
import java.util.List;
import java.util.Optional;

public interface UsuarioPersistencePort {
    List<Usuario> findAll();
    Optional<Usuario> findById(Long id);
    Optional<Usuario> findByCorreo(String correo);
    boolean existsByCorreo(String correo);
    boolean existsById(Long id);
    Usuario save(Usuario usuario);
    void deleteById(Long id);
}
