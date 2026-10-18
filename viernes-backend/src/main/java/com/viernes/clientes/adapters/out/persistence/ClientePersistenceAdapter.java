package com.viernes.clientes.adapters.out.persistence;

import com.viernes.clientes.adapters.out.persistence.entity.ClienteEntity;
import com.viernes.clientes.adapters.out.persistence.entity.ContactoEntity;
import com.viernes.clientes.adapters.out.persistence.repository.ClienteSpringDataRepository;
import com.viernes.clientes.application.port.out.ClientePersistencePort;
import com.viernes.clientes.domain.Cliente;
import com.viernes.clientes.domain.Contacto;
import com.viernes.clientes.domain.EstadoCliente;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ClientePersistenceAdapter implements ClientePersistencePort {

    private final ClienteSpringDataRepository repository;

    @Override
    public List<Cliente> findAll() {
        return repository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public List<Cliente> findByNombreOrEmpresa(String q) {
        return repository.findByNombreContainingIgnoreCaseOrEmpresaContainingIgnoreCase(q, q)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<Cliente> findByEstado(EstadoCliente estado) {
        return repository.findByEstado(estado).stream().map(this::toDomain).toList();
    }

    @Override
    public long countByEstado(EstadoCliente estado) {
        return repository.countByEstado(estado);
    }

    @Override
    public Optional<Cliente> findById(Long id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Cliente save(Cliente cliente) {
        ClienteEntity entity = toEntity(cliente);
        return toDomain(repository.save(entity));
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    private Cliente toDomain(ClienteEntity entity) {
        List<Contacto> contactos = entity.getContactos() != null ?
                entity.getContactos().stream().map(this::toContactoDomain)
                        .collect(Collectors.toCollection(ArrayList::new)) : new ArrayList<>();
                
        return Cliente.builder()
                .id(entity.getId())
                .nombre(entity.getNombre())
                .empresa(entity.getEmpresa())
                .correo(entity.getCorreo())
                .telefono(entity.getTelefono())
                .estado(entity.getEstado())
                .responsable(entity.getResponsable())
                .creadoEn(entity.getCreadoEn())
                .contactos(contactos)
                .build();
    }

    private ClienteEntity toEntity(Cliente domain) {
        ClienteEntity entity = ClienteEntity.builder()
                .id(domain.getId())
                .nombre(domain.getNombre())
                .empresa(domain.getEmpresa())
                .correo(domain.getCorreo())
                .telefono(domain.getTelefono())
                .estado(domain.getEstado())
                .responsable(domain.getResponsable())
                .creadoEn(domain.getCreadoEn())
                .build();

        List<ContactoEntity> contactosEntities = domain.getContactos() != null ?
                domain.getContactos().stream().map(c -> toContactoEntity(c, entity))
                        .collect(Collectors.toCollection(ArrayList::new)) : new ArrayList<>();
        entity.setContactos(contactosEntities);
        return entity;
    }

    private Contacto toContactoDomain(ContactoEntity entity) {
        return Contacto.builder()
                .id(entity.getId())
                .idCliente(entity.getCliente() != null ? entity.getCliente().getId() : entity.getIdCliente())
                .nombre(entity.getNombre())
                .apellido(entity.getApellido())
                .cargo(entity.getCargo())
                .correo(entity.getCorreo())
                .telefono(entity.getTelefono())
                .principal(entity.getPrincipal())
                .creadoEn(entity.getCreadoEn())
                .build();
    }

    private ContactoEntity toContactoEntity(Contacto domain, ClienteEntity cliente) {
        return ContactoEntity.builder()
                .id(domain.getId())
                .cliente(cliente)
                .nombre(domain.getNombre())
                .apellido(domain.getApellido())
                .cargo(domain.getCargo())
                .correo(domain.getCorreo())
                .telefono(domain.getTelefono())
                .principal(domain.getPrincipal())
                .creadoEn(domain.getCreadoEn())
                .build();
    }
}
