package com.viernes.oportunidades.application.port.out;

import com.viernes.clientes.application.dto.ContactoResponse;
import java.util.Optional;

public interface ContactoQueryPort {

    Optional<ContactoResponse> buscarPorId(Long idContacto);
}
