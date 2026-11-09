package com.viernes.clientes.application.service;

import com.viernes.clientes.application.dto.ClienteRequest;
import com.viernes.clientes.application.dto.ClienteResponse;
import com.viernes.clientes.application.dto.ContactoRequest;
import com.viernes.clientes.application.dto.ContactoResponse;
import com.viernes.clientes.application.dto.DocumentoConsultaResponse;
import com.viernes.clientes.application.port.in.ManageClienteUseCase;
import com.viernes.clientes.application.port.out.ClientePersistencePort;
import com.viernes.clientes.application.port.out.DocumentoLookupPort;
import com.viernes.clientes.domain.Cliente;
import com.viernes.clientes.domain.Contacto;
import com.viernes.clientes.domain.DocumentoConsultado;
import com.viernes.clientes.domain.EstadoCliente;
import com.viernes.clientes.domain.TipoDocumento;
import com.viernes.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClienteService implements ManageClienteUseCase {

    private final ClientePersistencePort persistencePort;
    private final DocumentoLookupPort documentoLookupPort;

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponse> listar() {
        return persistencePort.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponse> buscar(String q) {
        return persistencePort.findByNombreOrEmpresa(q).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponse> buscarPorDocumento(String numeroDocumento) {
        if (numeroDocumento == null || numeroDocumento.isBlank()) {
            return List.of();
        }
        return persistencePort.findByNumeroDocumento(numeroDocumento.trim()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponse> filtrarPorEstado(EstadoCliente estado) {
        return persistencePort.findByEstado(estado).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponse obtener(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentoConsultaResponse consultarDocumento(TipoDocumento tipoDocumento, String numeroDocumento) {
        String numeroLimpio = validarDocumento(tipoDocumento, numeroDocumento);
        if (numeroLimpio == null) {
            throw new IllegalArgumentException("Debe ingresar el número de documento");
        }

        DocumentoConsultado documento = documentoLookupPort.consultar(tipoDocumento, numeroLimpio);
        return toDocumentoResponse(documento);
    }

    @Override
    @Transactional
    public ClienteResponse crear(ClienteRequest request) {
        String numeroDocumento = validarDocumento(request.tipoDocumento(), request.numeroDocumento());
        validarDocumentoUnico(numeroDocumento, null);

        Cliente c = Cliente.builder()
            .creadoEn(LocalDateTime.now())
            .estado(request.estado() != null ? request.estado() : EstadoCliente.ACTIVO)
            .build();
        apply(c, request);
        return toResponse(persistencePort.save(c));
    }

    @Override
    @Transactional
    public ClienteResponse actualizar(Long id, ClienteRequest request) {
        Cliente c = findOrThrow(id);
        String numeroDocumento = validarDocumento(request.tipoDocumento(), request.numeroDocumento());
        validarDocumentoUnico(numeroDocumento, id);

        apply(c, request);
        return toResponse(persistencePort.save(c));
    }

    @Override
    @Transactional
    public void desactivar(Long id) {
        Cliente c = findOrThrow(id);
        c.setEstado(EstadoCliente.INACTIVO);
        persistencePort.save(c);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        findOrThrow(id);
        persistencePort.deleteById(id);
    }

    @Override
    @Transactional
    public ContactoResponse agregarContacto(Long clienteId, ContactoRequest request) {
        Cliente cliente = findOrThrow(clienteId);
        
        Contacto contacto = Contacto.builder()
                .idCliente(clienteId)
                .nombre(request.nombre())
                .apellido(request.apellido())
                .cargo(request.cargo())
                .correo(request.correo())
                .telefono(request.telefono())
                .principal(request.principal() != null ? request.principal() : false)
                .creadoEn(LocalDateTime.now())
                .build();
                
        cliente.getContactos().add(contacto);
        Cliente clienteGuardado = persistencePort.save(cliente);
        
        // El nuevo contacto será el último agregado a la lista del cliente (o lo buscamos por algún campo temporal)
        // JPA asignará el ID tras el save(). Asumiremos que es el de fecha más reciente o el último de la lista.
        Contacto contactoGuardado = clienteGuardado.getContactos().get(clienteGuardado.getContactos().size() - 1);
        return toContactoResponse(contactoGuardado);
    }

    @Override
    @Transactional
    public ContactoResponse actualizarContacto(Long clienteId, Long contactoId, ContactoRequest request) {
        Cliente cliente = findOrThrow(clienteId);
        
        Contacto contacto = cliente.getContactos().stream()
                .filter(c -> c.getId().equals(contactoId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Contacto no encontrado: " + contactoId));
                
        contacto.setNombre(request.nombre());
        contacto.setApellido(request.apellido());
        contacto.setCargo(request.cargo());
        contacto.setCorreo(request.correo());
        contacto.setTelefono(request.telefono());
        if (request.principal() != null) contacto.setPrincipal(request.principal());
        
        persistencePort.save(cliente);
        return toContactoResponse(contacto);
    }

    @Override
    @Transactional
    public void eliminarContacto(Long clienteId, Long contactoId) {
        Cliente cliente = findOrThrow(clienteId);
        
        boolean removido = cliente.getContactos().removeIf(c -> c.getId().equals(contactoId));
        if (!removido) {
            throw new ResourceNotFoundException("Contacto no encontrado: " + contactoId);
        }
        
        persistencePort.save(cliente);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContactoResponse> listarContactos(Long clienteId) {
        Cliente cliente = findOrThrow(clienteId);
        return cliente.getContactos().stream()
                .map(this::toContactoResponse)
                .toList();
    }

    private Cliente findOrThrow(Long id) {
        return persistencePort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado: " + id));
    }

    private void apply(Cliente c, ClienteRequest r) {
        c.setNombre(r.nombre());
        c.setEmpresa(r.empresa());
        c.setTipoDocumento(r.tipoDocumento());
        c.setNumeroDocumento(validarDocumento(r.tipoDocumento(), r.numeroDocumento()));
        c.setCorreo(r.correo());
        c.setTelefono(r.telefono());
        c.setResponsable(r.responsable());
        if (r.estado() != null) c.setEstado(r.estado());
    }

    private String validarDocumento(TipoDocumento tipoDocumento, String numeroDocumento) {
        boolean tieneTipo = tipoDocumento != null;
        boolean tieneNumero = numeroDocumento != null && !numeroDocumento.isBlank();

        if (!tieneTipo && !tieneNumero) {
            return null;
        }
        if (!tieneTipo) {
            throw new IllegalArgumentException("Debe seleccionar el tipo de documento");
        }
        if (!tieneNumero) {
            throw new IllegalArgumentException("Debe ingresar el número de documento");
        }

        String numeroLimpio = numeroDocumento.trim();
        if (tipoDocumento == TipoDocumento.DNI && !numeroLimpio.matches("\\d{8}")) {
            throw new IllegalArgumentException("El DNI debe tener exactamente 8 dígitos");
        }
        if (tipoDocumento == TipoDocumento.RUC && !numeroLimpio.matches("\\d{11}")) {
            throw new IllegalArgumentException("El RUC debe tener exactamente 11 dígitos");
        }
        return numeroLimpio;
    }

    private void validarDocumentoUnico(String numeroDocumento, Long id) {
        if (numeroDocumento == null) {
            return;
        }
        boolean existe = id == null
                ? persistencePort.existsByNumeroDocumento(numeroDocumento)
                : persistencePort.existsByNumeroDocumentoAndIdNot(numeroDocumento, id);
        if (existe) {
            throw new IllegalArgumentException("Ya existe un cliente con el documento: " + numeroDocumento);
        }
    }

    private DocumentoConsultaResponse toDocumentoResponse(DocumentoConsultado documento) {
        return new DocumentoConsultaResponse(
                documento.tipoDocumento(),
                documento.numeroDocumento(),
                documento.nombreCompleto(),
                documento.razonSocial(),
                documento.nombreComercial(),
                documento.estado(),
                documento.condicion(),
                documento.direccion(),
                documento.telefonos()
        );
    }

    private ClienteResponse toResponse(Cliente c) {
        List<ContactoResponse> contactosDto = c.getContactos() != null ? 
                c.getContactos().stream().map(this::toContactoResponse).toList() : List.of();
                
        return new ClienteResponse(c.getId(), c.getNombre(), c.getEmpresa(),
                c.getTipoDocumento(), c.getNumeroDocumento(),
                c.getCorreo(), c.getTelefono(), c.getEstado(), c.getResponsable(), c.getCreadoEn(), contactosDto);
    }

    private ContactoResponse toContactoResponse(Contacto c) {
        return new ContactoResponse(c.getId(), c.getIdCliente(), c.getNombre(), c.getApellido(),
                c.getCargo(), c.getCorreo(), c.getTelefono(), c.getPrincipal(), c.getCreadoEn());
    }
}
