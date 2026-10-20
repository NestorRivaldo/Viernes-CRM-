package com.viernes.usuarios;

import java.util.Optional;

/**
 * API pública del módulo Usuarios.
 * Único contrato que otros módulos pueden consumir (named interface por
 * defecto de Spring Modulith: los tipos del paquete raíz del módulo).
 */
public interface UsuariosApi {

    Optional<UsuarioResumen> buscarPorId(Long id);

    Optional<UsuarioResumen> buscarPorCorreo(String correo);

    boolean existePorId(Long id);

    boolean existePorCorreo(String correo);
}
