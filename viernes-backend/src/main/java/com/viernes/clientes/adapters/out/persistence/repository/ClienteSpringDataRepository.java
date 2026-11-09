package com.viernes.clientes.adapters.out.persistence.repository;

import com.viernes.clientes.adapters.out.persistence.entity.ClienteEntity;
import com.viernes.clientes.domain.EstadoCliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ClienteSpringDataRepository extends JpaRepository<ClienteEntity, Long> {
    List<ClienteEntity> findByNombreContainingIgnoreCaseOrEmpresaContainingIgnoreCaseOrNumeroDocumentoContainingIgnoreCase(
            String nombre, String empresa, String numeroDocumento);
    List<ClienteEntity> findByNumeroDocumento(String numeroDocumento);
    boolean existsByNumeroDocumento(String numeroDocumento);
    boolean existsByNumeroDocumentoAndIdNot(String numeroDocumento, Long id);
    List<ClienteEntity> findByEstado(EstadoCliente estado);
    long countByEstado(EstadoCliente estado);
}
