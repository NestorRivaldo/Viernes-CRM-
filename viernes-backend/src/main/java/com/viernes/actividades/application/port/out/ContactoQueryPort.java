package com.viernes.actividades.application.port.out;

import com.viernes.clientes.application.dto.ContactoResponse;
import java.util.Optional;

/**
 * Puerto de salida para consultar contactos del módulo Clientes.
 * Respeta la Arquitectura Hexagonal y Spring Modulith: el servicio de
 * actividades depende de esta abstracción y el adapter consume únicamente
 * el caso de uso público de Clientes (ManageClienteUseCase).
 */
public interface ContactoQueryPort {

    Optional<ContactoResponse> buscarPorId(Long idContacto);
}
