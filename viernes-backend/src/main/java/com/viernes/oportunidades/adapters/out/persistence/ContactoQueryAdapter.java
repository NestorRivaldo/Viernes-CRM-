package com.viernes.oportunidades.adapters.out.persistence;

import com.viernes.clientes.application.dto.ContactoResponse;
import com.viernes.clientes.application.port.in.ManageClienteUseCase;
import com.viernes.oportunidades.application.port.out.ContactoQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component("oportunidadContactoQueryAdapter")
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
