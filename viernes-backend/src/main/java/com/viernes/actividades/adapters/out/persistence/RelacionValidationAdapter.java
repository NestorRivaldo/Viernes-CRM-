package com.viernes.actividades.adapters.out.persistence;

import com.viernes.actividades.application.port.out.RelacionValidationPort;
import com.viernes.clientes.adapters.out.persistence.repository.ClienteSpringDataRepository;
import com.viernes.leads.adapters.out.persistence.repository.LeadSpringDataRepository;
import com.viernes.oportunidades.adapters.out.persistence.repository.OportunidadSpringDataRepository;
import com.viernes.usuarios.UsuariosApi;
import org.springframework.stereotype.Component;

/**
 * Adaptador que implementa la validación de existencia de entidades externas.
 *
 * Consulta los repositorios de Clientes, Leads y Oportunidades (módulos OPEN)
 * y la API pública del módulo Usuarios para verificar que las referencias
 * proporcionadas existen en la base de datos.
 */
@Component
public class RelacionValidationAdapter implements RelacionValidationPort {

    private final ClienteSpringDataRepository clienteRepository;
    private final LeadSpringDataRepository leadRepository;
    private final OportunidadSpringDataRepository oportunidadRepository;
    private final UsuariosApi usuariosApi;

    public RelacionValidationAdapter(
            ClienteSpringDataRepository clienteRepository,
            LeadSpringDataRepository leadRepository,
            OportunidadSpringDataRepository oportunidadRepository,
            UsuariosApi usuariosApi) {
        this.clienteRepository = clienteRepository;
        this.leadRepository = leadRepository;
        this.oportunidadRepository = oportunidadRepository;
        this.usuariosApi = usuariosApi;
    }

    @Override
    public boolean existeCliente(Long idCliente) {
        return clienteRepository.existsById(idCliente);
    }

    @Override
    public boolean existeLead(Long idLead) {
        return leadRepository.existsById(idLead);
    }

    @Override
    public boolean existeOportunidad(Long idOportunidad) {
        return oportunidadRepository.existsById(idOportunidad);
    }

    @Override
    public boolean existeUsuario(String idUsuario) {
        try {
            return usuariosApi.existePorId(Long.parseLong(idUsuario));
        } catch (NumberFormatException e) {
            return usuariosApi.existePorCorreo(idUsuario);
        }
    }
}
