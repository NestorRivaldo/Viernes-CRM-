package com.viernes.clientes.application.port.in;

import com.viernes.clientes.application.dto.ClienteRequest;
import com.viernes.clientes.application.dto.ClienteResponse;
import com.viernes.clientes.application.dto.ContactoRequest;
import com.viernes.clientes.application.dto.ContactoResponse;
import com.viernes.clientes.domain.EstadoCliente;
import java.util.List;

public interface ManageClienteUseCase {
    List<ClienteResponse> listar();
    List<ClienteResponse> buscar(String q);
    List<ClienteResponse> filtrarPorEstado(EstadoCliente estado);
    ClienteResponse obtener(Long id);
    ClienteResponse crear(ClienteRequest request);
    ClienteResponse actualizar(Long id, ClienteRequest request);
    void desactivar(Long id);
    void eliminar(Long id);

    ContactoResponse agregarContacto(Long clienteId, ContactoRequest request);
    ContactoResponse actualizarContacto(Long clienteId, Long contactoId, ContactoRequest request);
    void eliminarContacto(Long clienteId, Long contactoId);
    List<ContactoResponse> listarContactos(Long clienteId);
}
