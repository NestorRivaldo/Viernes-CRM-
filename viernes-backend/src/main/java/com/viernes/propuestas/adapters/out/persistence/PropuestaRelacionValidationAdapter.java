package com.viernes.propuestas.adapters.out.persistence;

import com.viernes.propuestas.application.port.out.PropuestaRelacionValidationPort;
import com.viernes.clientes.adapters.out.persistence.repository.ClienteSpringDataRepository;
import com.viernes.oportunidades.adapters.out.persistence.repository.OportunidadSpringDataRepository;
import org.springframework.stereotype.Component;

/**
 * Adaptador que valida la existencia de entidades externas (Oportunidad, Cliente).
 * Sigue el mismo patrón establecido por RelacionValidationAdapter en el módulo de Actividades.
 */
@Component
public class PropuestaRelacionValidationAdapter implements PropuestaRelacionValidationPort {

    private final OportunidadSpringDataRepository oportunidadRepository;
    private final ClienteSpringDataRepository clienteRepository;

    public PropuestaRelacionValidationAdapter(
            OportunidadSpringDataRepository oportunidadRepository,
            ClienteSpringDataRepository clienteRepository) {
        this.oportunidadRepository = oportunidadRepository;
        this.clienteRepository = clienteRepository;
    }

    @Override
    public boolean existeOportunidad(Long idOportunidad) {
        return oportunidadRepository.existsById(idOportunidad);
    }

    @Override
    public boolean existeCliente(Long idCliente) {
        return clienteRepository.existsById(idCliente);
    }
}
