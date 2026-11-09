package com.viernes.oportunidades.application.service;

import com.viernes.oportunidades.application.dto.OportunidadColaboradorRequest;
import com.viernes.oportunidades.application.dto.OportunidadColaboradorResponse;
import com.viernes.oportunidades.application.dto.OportunidadContactoRequest;
import com.viernes.oportunidades.application.dto.OportunidadContactoResponse;
import com.viernes.oportunidades.application.dto.OportunidadContactoUpdateRequest;
import com.viernes.oportunidades.application.dto.OportunidadProductoRequest;
import com.viernes.oportunidades.application.dto.OportunidadProductoResponse;
import com.viernes.oportunidades.application.dto.OportunidadRequest;
import com.viernes.oportunidades.application.dto.OportunidadResponse;
import com.viernes.oportunidades.application.port.in.ManageOportunidadUseCase;
import com.viernes.oportunidades.application.port.out.ContactoQueryPort;
import com.viernes.oportunidades.application.port.out.OportunidadColaboradorPersistencePort;
import com.viernes.oportunidades.application.port.out.OportunidadContactoPersistencePort;
import com.viernes.oportunidades.application.port.out.OportunidadPersistencePort;
import com.viernes.oportunidades.application.port.out.OportunidadProductoPersistencePort;
import com.viernes.oportunidades.application.port.out.ProductoQueryPort;
import com.viernes.oportunidades.application.port.out.UsuarioQueryPort;
import com.viernes.oportunidades.domain.EtapaOportunidad;
import com.viernes.oportunidades.domain.Oportunidad;
import com.viernes.oportunidades.domain.OportunidadColaborador;
import com.viernes.oportunidades.domain.OportunidadContacto;
import com.viernes.oportunidades.domain.OportunidadProducto;
import com.viernes.clientes.application.dto.ContactoResponse;
import com.viernes.productos.application.dto.ProductoResponse;
import com.viernes.shared.exception.ResourceNotFoundException;
import com.viernes.usuarios.UsuarioResumen;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OportunidadService implements ManageOportunidadUseCase {

    private final OportunidadPersistencePort persistencePort;
    private final OportunidadContactoPersistencePort contactoPort;
    private final OportunidadProductoPersistencePort productoPort;
    private final OportunidadColaboradorPersistencePort colaboradorPort;
    private final ContactoQueryPort contactoQueryPort;
    private final ProductoQueryPort productoQueryPort;
    private final UsuarioQueryPort usuarioQueryPort;

    @Override
    public List<OportunidadResponse> listar() {
        return persistencePort.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public List<OportunidadResponse> buscar(String q) {
        return persistencePort.findByNombreOrClienteNombre(q).stream().map(this::toResponse).toList();
    }

    @Override
    public List<OportunidadResponse> filtrarPorEtapa(EtapaOportunidad etapa) {
        return persistencePort.findByEtapa(etapa).stream().map(this::toResponse).toList();
    }

    @Override
    public OportunidadResponse obtener(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    @Transactional
    public OportunidadResponse crear(OportunidadRequest request) {
        Oportunidad o = Oportunidad.builder()
            .creadoEn(LocalDateTime.now())
            .etapa(request.etapa() != null ? request.etapa() : EtapaOportunidad.PROSPECCION)
            .build();
        apply(o, request);
        return toResponse(persistencePort.save(o));
    }

    @Override
    @Transactional
    public OportunidadResponse actualizar(Long id, OportunidadRequest request) {
        Oportunidad o = findOrThrow(id);
        apply(o, request);
        return toResponse(persistencePort.save(o));
    }

    @Override
    @Transactional
    public OportunidadResponse cambiarEtapa(Long id, EtapaOportunidad etapa) {
        Oportunidad o = findOrThrow(id);
        o.setEtapa(etapa);
        return toResponse(persistencePort.save(o));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        findOrThrow(id);
        contactoPort.deleteByIdOportunidad(id);
        productoPort.deleteByIdOportunidad(id);
        colaboradorPort.deleteByIdOportunidad(id);
        persistencePort.deleteById(id);
    }

    // --- Contactos de la oportunidad ---

    @Override
    @Transactional
    public OportunidadContactoResponse agregarContacto(Long id, OportunidadContactoRequest request) {
        findOrThrow(id);
        ContactoResponse contacto = contactoQueryPort.buscarPorId(request.contactoId())
                .orElseThrow(() -> new ResourceNotFoundException("Contacto no encontrado: " + request.contactoId()));
        if (contactoPort.existsByIdOportunidadAndIdContacto(id, request.contactoId())) {
            throw new IllegalArgumentException("El contacto ya está asociado a la oportunidad");
        }
        OportunidadContacto asociacion = OportunidadContacto.builder()
                .idOportunidad(id)
                .idContacto(request.contactoId())
                .rol(request.rol())
                .esDecisor(request.esDecisor() != null ? request.esDecisor() : false)
                .creadoEn(LocalDateTime.now())
                .build();
        return toContactoResponse(contactoPort.save(asociacion), contacto);
    }

    @Override
    @Transactional
    public OportunidadContactoResponse actualizarContacto(Long id, Long contactoId, OportunidadContactoUpdateRequest request) {
        OportunidadContacto asociacion = contactoPort.findByIdOportunidadAndIdContacto(id, contactoId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El contacto " + contactoId + " no está asociado a la oportunidad " + id));
        if (request.rol() != null) asociacion.setRol(request.rol());
        if (request.esDecisor() != null) asociacion.setEsDecisor(request.esDecisor());
        OportunidadContacto saved = contactoPort.save(asociacion);
        return toContactoResponse(saved, contactoQueryPort.buscarPorId(contactoId).orElse(null));
    }

    @Override
    @Transactional
    public void quitarContacto(Long id, Long contactoId) {
        OportunidadContacto asociacion = contactoPort.findByIdOportunidadAndIdContacto(id, contactoId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El contacto " + contactoId + " no está asociado a la oportunidad " + id));
        contactoPort.delete(asociacion);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OportunidadContactoResponse> listarContactos(Long id) {
        findOrThrow(id);
        return contactoPort.findByIdOportunidad(id).stream()
                .map(c -> toContactoResponse(c, contactoQueryPort.buscarPorId(c.getIdContacto()).orElse(null)))
                .toList();
    }

    // --- Productos de la oportunidad ---

    @Override
    @Transactional
    public OportunidadProductoResponse agregarProducto(Long id, OportunidadProductoRequest request) {
        findOrThrow(id);
        ProductoResponse producto = productoQueryPort.buscarPorId(request.productoId())
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + request.productoId()));
        if (productoPort.existsByIdOportunidadAndIdProducto(id, request.productoId())) {
            throw new IllegalArgumentException("El producto ya está asociado a la oportunidad");
        }
        OportunidadProducto asociacion = OportunidadProducto.builder()
                .idOportunidad(id)
                .idProducto(request.productoId())
                .cantidad(request.cantidad())
                .creadoEn(LocalDateTime.now())
                .build();
        return toProductoResponse(productoPort.save(asociacion), producto);
    }

    @Override
    @Transactional
    public void quitarProducto(Long id, Long productoId) {
        OportunidadProducto asociacion = productoPort.findByIdOportunidadAndIdProducto(id, productoId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El producto " + productoId + " no está asociado a la oportunidad " + id));
        productoPort.delete(asociacion);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OportunidadProductoResponse> listarProductos(Long id) {
        findOrThrow(id);
        return productoPort.findByIdOportunidad(id).stream()
                .map(p -> toProductoResponse(p, productoQueryPort.buscarPorId(p.getIdProducto()).orElse(null)))
                .toList();
    }

    // --- Colaboradores de la oportunidad ---

    @Override
    @Transactional
    public OportunidadColaboradorResponse agregarColaborador(Long id, OportunidadColaboradorRequest request) {
        findOrThrow(id);
        UsuarioResumen usuario = usuarioQueryPort.buscarPorId(request.usuarioId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + request.usuarioId()));
        if (colaboradorPort.existsByIdOportunidadAndIdUsuario(id, request.usuarioId())) {
            throw new IllegalArgumentException("El usuario ya es colaborador de la oportunidad");
        }
        OportunidadColaborador asociacion = OportunidadColaborador.builder()
                .idOportunidad(id)
                .idUsuario(request.usuarioId())
                .creadoEn(LocalDateTime.now())
                .build();
        return toColaboradorResponse(colaboradorPort.save(asociacion), usuario);
    }

    @Override
    @Transactional
    public void quitarColaborador(Long id, Long usuarioId) {
        OportunidadColaborador asociacion = colaboradorPort.findByIdOportunidadAndIdUsuario(id, usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El usuario " + usuarioId + " no es colaborador de la oportunidad " + id));
        colaboradorPort.delete(asociacion);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OportunidadColaboradorResponse> listarColaboradores(Long id) {
        findOrThrow(id);
        return colaboradorPort.findByIdOportunidad(id).stream()
                .map(c -> toColaboradorResponse(c, usuarioQueryPort.buscarPorId(c.getIdUsuario()).orElse(null)))
                .toList();
    }

    private Oportunidad findOrThrow(Long id) {
        return persistencePort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Oportunidad no encontrada: " + id));
    }

    private void apply(Oportunidad o, OportunidadRequest r) {
        o.setNombre(r.nombre());
        o.setClienteNombre(r.clienteNombre());
        o.setValor(r.valor());
        o.setProbabilidad(r.probabilidad());
        o.setResponsable(r.responsable());
        o.setFechaCierre(r.fechaCierre());
        o.setDescripcion(r.descripcion());
        if (r.etapa() != null) o.setEtapa(r.etapa());
    }

    private OportunidadResponse toResponse(Oportunidad o) {
        return new OportunidadResponse(o.getId(), o.getNombre(), o.getClienteNombre(),
                o.getValor(), o.getProbabilidad(), o.getValorPonderado(),
                o.getEtapa(), o.getResponsable(), o.getFechaCierre(), o.getDescripcion(), o.getCreadoEn());
    }

    private OportunidadContactoResponse toContactoResponse(OportunidadContacto c, ContactoResponse contacto) {
        String nombre = null;
        String correo = null;
        if (contacto != null) {
            nombre = contacto.apellido() != null && !contacto.apellido().isBlank()
                    ? contacto.nombre() + " " + contacto.apellido()
                    : contacto.nombre();
            correo = contacto.correo();
        }
        return new OportunidadContactoResponse(c.getId(), c.getIdOportunidad(), c.getIdContacto(),
                nombre, correo, c.getRol(), c.getEsDecisor(), c.getCreadoEn());
    }

    private OportunidadProductoResponse toProductoResponse(OportunidadProducto p, ProductoResponse producto) {
        return new OportunidadProductoResponse(p.getId(), p.getIdOportunidad(), p.getIdProducto(),
                producto != null ? producto.codigo() : null,
                producto != null ? producto.nombre() : null,
                producto != null ? producto.precioBase() : null,
                p.getCantidad(), p.getCreadoEn());
    }

    private OportunidadColaboradorResponse toColaboradorResponse(OportunidadColaborador c, UsuarioResumen usuario) {
        String nombreCompleto = null;
        String correo = null;
        if (usuario != null) {
            nombreCompleto = usuario.apellido() != null && !usuario.apellido().isBlank()
                    ? usuario.nombre() + " " + usuario.apellido()
                    : usuario.nombre();
            correo = usuario.correo();
        }
        return new OportunidadColaboradorResponse(c.getId(), c.getIdOportunidad(), c.getIdUsuario(),
                nombreCompleto, correo, c.getCreadoEn());
    }
}
