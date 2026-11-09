package com.viernes.clientes.application.port.out;

import com.viernes.clientes.domain.Cliente;
import com.viernes.clientes.domain.EstadoCliente;
import java.util.List;
import java.util.Optional;

public interface ClientePersistencePort {
    List<Cliente> findAll();
    List<Cliente> findByNombreOrEmpresa(String q);
    List<Cliente> findByNumeroDocumento(String numeroDocumento);
    boolean existsByNumeroDocumento(String numeroDocumento);
    boolean existsByNumeroDocumentoAndIdNot(String numeroDocumento, Long id);
    List<Cliente> findByEstado(EstadoCliente estado);
    long countByEstado(EstadoCliente estado);
    Optional<Cliente> findById(Long id);
    Cliente save(Cliente cliente);
    void deleteById(Long id);
}
