package com.viernes.actividades.application.port.out;

/**
 * Puerto de salida para validar la existencia de entidades externas al módulo.
 * Respeta la Arquitectura Hexagonal: el servicio de aplicación depende de esta
 * abstracción y no de los módulos externos directamente.
 *
 * La implementación concreta (adapter) consulta los módulos de Clientes,
 * Leads, Oportunidades y Usuarios según corresponda.
 */
public interface RelacionValidationPort {

    boolean existeCliente(Long idCliente);

    boolean existeLead(Long idLead);

    boolean existeOportunidad(Long idOportunidad);

    /**
     * Valida si el usuario existe en el módulo Usuarios.
     * El identificador puede ser el ID numérico o el correo del usuario.
     */
    boolean existeUsuario(String idUsuario);
}
