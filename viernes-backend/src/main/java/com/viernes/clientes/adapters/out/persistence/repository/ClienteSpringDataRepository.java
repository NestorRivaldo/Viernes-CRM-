package com.viernes.clientes.adapters.out.persistence.repository;

import com.viernes.clientes.adapters.out.persistence.entity.ClienteEntity;
import com.viernes.clientes.domain.EstadoCliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ClienteSpringDataRepository extends JpaRepository<ClienteEntity, Long> {
    List<ClienteEntity> findByNombreContainingIgnoreCaseOrEmpresaContainingIgnoreCase(String nombre, String empresa);
    List<ClienteEntity> findByEstado(EstadoCliente estado);
    long countByEstado(EstadoCliente estado);
}
