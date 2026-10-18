package com.viernes.usuarios.application.port.in;

/**
 * Port de consulta pública del módulo Usuarios.
 * Permite que otros módulos verifiquen la existencia de un usuario
 * sin acceder directamente a la infraestructura interna.
 */
public interface UsuarioConsultaPort {
    boolean existePorId(Long id);
    boolean existePorCorreo(String correo);
}
