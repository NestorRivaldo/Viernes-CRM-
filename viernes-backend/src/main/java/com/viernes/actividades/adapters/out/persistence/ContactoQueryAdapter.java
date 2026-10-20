package com.viernes.actividades.adapters.out.persistence;

import com.viernes.actividades.application.port.out.ContactoQueryPort;
import com.viernes.clientes.application.dto.ContactoResponse;
import com.viernes.clientes.application.port.in.ManageClienteUseCase;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Adaptador que consulta contactos usando solo el puerto público del
 * módulo Clientes (ManageClienteUseCase). Nunca accede a entidades ni
 * repositorios de Clientes.
 *
 * Nota: el puerto público de Clientes no expone una búsqueda directa por
 * ID de contacto, por lo que se recorren los clientes y sus contactos.
 * Cuando Clientes exponga un método obtenerContacto(contactoId), este
 * adapter es el único punto a actualizar.
 */
@Component
public class ContactoQueryAdapter implements ContactoQueryPort {

    private final ManageClienteUseCase clienteUseCase;

    public ContactoQueryAdapter(ManageClienteUseCase clienteUseCase) {
        this.clienteUseCase = clienteUseCase;
    }

    @Override
    public Optional<ContactoResponse> buscarPorId(Long idContacto) {
        return clienteUseCase.listar().stream()
                .flatMap(cliente -> cliente.contactos().stream())
                .filter(contacto -> contacto.id().equals(idContacto))
                .findFirst();
    }
}
