package com.viernes.equipos.application.port.out;

import com.viernes.usuarios.UsuarioResumen;
import java.util.Optional;

/**
 * Puerto de salida para validar la existencia de usuarios en el módulo Usuarios.
 * La implementación (adapter) consume la API pública de Usuarios; el servicio
 * de aplicación no depende del módulo externo directamente.
 */
public interface UsuarioValidationPort {

    boolean existeUsuario(Long usuarioId);

    Optional<UsuarioResumen> buscarUsuario(Long usuarioId);
}
