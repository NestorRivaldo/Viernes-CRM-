package com.viernes.propuestas.application.port.out;

/**
 * Puerto de salida para validar la existencia de entidades externas al módulo.
 * Sigue el patrón establecido por RelacionValidationPort en el módulo de Actividades.
 */
public interface PropuestaRelacionValidationPort {
    boolean existeOportunidad(Long idOportunidad);
    boolean existeCliente(Long idCliente);
}
