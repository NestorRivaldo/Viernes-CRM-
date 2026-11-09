package com.viernes.relationshipmap.application.service;

import com.viernes.actividades.application.dto.ActividadResponse;
import com.viernes.actividades.application.dto.ParticipanteResponse;
import com.viernes.actividades.application.port.in.ManageActividadUseCase;
import com.viernes.clientes.application.dto.ClienteResponse;
import com.viernes.clientes.application.dto.ContactoResponse;
import com.viernes.clientes.application.port.in.ManageClienteUseCase;
import com.viernes.oportunidades.application.dto.OportunidadColaboradorResponse;
import com.viernes.oportunidades.application.dto.OportunidadContactoResponse;
import com.viernes.oportunidades.application.dto.OportunidadProductoResponse;
import com.viernes.oportunidades.application.dto.OportunidadResponse;
import com.viernes.oportunidades.application.port.in.ManageOportunidadUseCase;
import com.viernes.productos.application.dto.ProductoResponse;
import com.viernes.productos.application.port.in.ManageProductoUseCase;
import com.viernes.propuestas.application.dto.DetallePropuestaResponse;
import com.viernes.propuestas.application.dto.PropuestaResponse;
import com.viernes.propuestas.application.port.in.ManagePropuestaUseCase;
import com.viernes.relationshipmap.application.dto.GraphEdgeDto;
import com.viernes.relationshipmap.application.dto.GraphNodeDto;
import com.viernes.relationshipmap.application.dto.RelationshipMapResponse;
import com.viernes.relationshipmap.application.port.in.ManageRelationshipMapUseCase;
import com.viernes.relationshipmap.domain.EdgeType;
import com.viernes.relationshipmap.domain.NodeType;
import com.viernes.shared.exception.ResourceNotFoundException;
import com.viernes.usuarios.UsuarioResumen;
import com.viernes.usuarios.UsuariosApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RelationshipMapService implements ManageRelationshipMapUseCase {

    private final ManageClienteUseCase clienteUseCase;
    private final ManageOportunidadUseCase oportunidadUseCase;
    private final ManageActividadUseCase actividadUseCase;
    private final ManagePropuestaUseCase propuestaUseCase;
    private final ManageProductoUseCase productoUseCase;
    private final UsuariosApi usuariosApi;

    @Override
    public RelationshipMapResponse obtenerMapaCliente(Long clienteId) {
        ClienteResponse cliente = clienteUseCase.obtener(clienteId);
        if (cliente == null) {
            throw new ResourceNotFoundException("Cliente no encontrado con id: " + clienteId);
        }

        Map<String, GraphNodeDto> nodes = new LinkedHashMap<>();
        Map<String, GraphEdgeDto> edges = new LinkedHashMap<>();

        // 1. Nodo Raíz: Cliente
        String rootId = makeNodeId(NodeType.CLIENTE, cliente.id());
        nodes.put(rootId, toClienteNode(cliente, 0));

        // 2. Contactos del cliente
        List<ContactoResponse> contactos = clienteUseCase.listarContactos(clienteId);
        for (ContactoResponse c : contactos) {
            String contactoNodeId = makeNodeId(NodeType.CONTACTO, c.id());
            nodes.put(contactoNodeId, toContactoNode(c));
            addEdge(edges, rootId, contactoNodeId, EdgeType.CLIENTE_CONTACTO, "Contacto", null);
        }

        // 3. Oportunidades asociadas
        List<OportunidadResponse> oportunidades = findOportunidadesPorCliente(cliente, contactos);
        for (OportunidadResponse o : oportunidades) {
            String oppNodeId = makeNodeId(NodeType.OPORTUNIDAD, o.id());
            nodes.put(oppNodeId, toOportunidadNode(o));
            addEdge(edges, rootId, oppNodeId, EdgeType.CLIENTE_OPORTUNIDAD, "Oportunidad", null);
        }

        // 4. Actividades asociadas
        List<ActividadResponse> actividades = actividadUseCase.filtrarPorCliente(clienteId);
        for (ActividadResponse a : actividades) {
            String actNodeId = makeNodeId(NodeType.ACTIVIDAD, a.id());
            nodes.put(actNodeId, toActividadNode(a));
            addEdge(edges, rootId, actNodeId, EdgeType.CLIENTE_ACTIVIDAD, "Actividad", null);
        }

        // 5. Propuestas asociadas
        List<PropuestaResponse> propuestas = propuestaUseCase.listarPorCliente(clienteId);
        for (PropuestaResponse p : propuestas) {
            String propNodeId = makeNodeId(NodeType.PROPUESTA, p.id());
            nodes.put(propNodeId, toPropuestaNode(p));
            addEdge(edges, rootId, propNodeId, EdgeType.CLIENTE_PROPUESTA, "Propuesta", null);
        }

        return new RelationshipMapResponse(
                rootId,
                new ArrayList<>(nodes.values()),
                new ArrayList<>(edges.values()),
                nodes.size(),
                edges.size()
        );
    }

    @Override
    public RelationshipMapResponse obtenerMapaOportunidad(Long oportunidadId) {
        OportunidadResponse opp = oportunidadUseCase.obtener(oportunidadId);
        if (opp == null) {
            throw new ResourceNotFoundException("Oportunidad no encontrada con id: " + oportunidadId);
        }

        Map<String, GraphNodeDto> nodes = new LinkedHashMap<>();
        Map<String, GraphEdgeDto> edges = new LinkedHashMap<>();

        // 1. Nodo Raíz: Oportunidad
        String rootId = makeNodeId(NodeType.OPORTUNIDAD, opp.id());
        nodes.put(rootId, toOportunidadNode(opp));

        // 2. Contactos de la oportunidad
        List<OportunidadContactoResponse> oppContactos = oportunidadUseCase.listarContactos(oportunidadId);
        for (OportunidadContactoResponse c : oppContactos) {
            String contactoNodeId = makeNodeId(NodeType.CONTACTO, c.contactoId());
            Map<String, Object> data = new HashMap<>();
            data.put("rol", c.rol());
            data.put("esDecisor", c.esDecisor());

            GraphNodeDto cNode = new GraphNodeDto(
                    contactoNodeId,
                    c.contactoId(),
                    NodeType.CONTACTO,
                    c.nombreContacto() != null ? c.nombreContacto() : "Contacto #" + c.contactoId(),
                    c.correoContacto() != null ? c.correoContacto() : (c.rol() != null ? c.rol() : "Contacto"),
                    "ACTIVO",
                    Boolean.TRUE.equals(c.esDecisor()) ? "Decisor" : (c.rol() != null ? c.rol() : ""),
                    true,
                    1,
                    data
            );
            nodes.put(contactoNodeId, cNode);
            addEdge(edges, rootId, contactoNodeId, EdgeType.OPORTUNIDAD_CONTACTO,
                    Boolean.TRUE.equals(c.esDecisor()) ? "Decisor" : "Contacto", data);
        }

        // 3. Cliente asociado (por nombre o deducido desde contactos)
        ClienteResponse cliente = findClientePorNombre(opp.clienteNombre());
        if (cliente == null && !oppContactos.isEmpty()) {
            for (OportunidadContactoResponse oc : oppContactos) {
                Optional<ContactoResponse> cOpt = findContactoById(oc.contactoId());
                if (cOpt.isPresent() && cOpt.get().idCliente() != null) {
                    try {
                        cliente = clienteUseCase.obtener(cOpt.get().idCliente());
                        if (cliente != null) break;
                    } catch (Exception ignored) {}
                }
            }
        }
        if (cliente != null) {
            String clienteNodeId = makeNodeId(NodeType.CLIENTE, cliente.id());
            nodes.put(clienteNodeId, toClienteNode(cliente, 1));
            addEdge(edges, rootId, clienteNodeId, EdgeType.CLIENTE_OPORTUNIDAD, "Cliente", null);
        }

        // 4. Productos de la oportunidad
        List<OportunidadProductoResponse> oppProductos = oportunidadUseCase.listarProductos(oportunidadId);
        for (OportunidadProductoResponse p : oppProductos) {
            String prodNodeId = makeNodeId(NodeType.PRODUCTO, p.productoId());
            Map<String, Object> data = new HashMap<>();
            data.put("cantidad", p.cantidad());
            data.put("precioBase", p.precioBase());

            GraphNodeDto pNode = new GraphNodeDto(
                    prodNodeId,
                    p.productoId(),
                    NodeType.PRODUCTO,
                    p.nombreProducto() != null ? p.nombreProducto() : "Producto #" + p.productoId(),
                    p.codigo() != null ? "Cód: " + p.codigo() : "",
                    "ACTIVO",
                    p.cantidad() != null ? p.cantidad() + " un." : "",
                    false,
                    1,
                    data
            );
            nodes.put(prodNodeId, pNode);
            addEdge(edges, rootId, prodNodeId, EdgeType.OPORTUNIDAD_PRODUCTO,
                    p.cantidad() != null ? p.cantidad() + " un." : "Producto", data);
        }

        // 5. Colaboradores de la oportunidad
        List<OportunidadColaboradorResponse> oppColabs = oportunidadUseCase.listarColaboradores(oportunidadId);
        for (OportunidadColaboradorResponse colab : oppColabs) {
            String userNodeId = makeNodeId(NodeType.USUARIO, colab.usuarioId());
            Map<String, Object> data = new HashMap<>();
            data.put("correo", colab.correo());

            GraphNodeDto uNode = new GraphNodeDto(
                    userNodeId,
                    colab.usuarioId(),
                    NodeType.USUARIO,
                    colab.nombreCompleto() != null ? colab.nombreCompleto() : "Usuario #" + colab.usuarioId(),
                    colab.correo() != null ? colab.correo() : "Colaborador",
                    "ACTIVO",
                    "Colaborador",
                    false,
                    1,
                    data
            );
            nodes.put(userNodeId, uNode);
            addEdge(edges, rootId, userNodeId, EdgeType.OPORTUNIDAD_COLABORADOR, "Colaborador", data);
        }

        // 6. Actividades de la oportunidad
        List<ActividadResponse> actividades = actividadUseCase.filtrarPorOportunidad(oportunidadId);
        for (ActividadResponse a : actividades) {
            String actNodeId = makeNodeId(NodeType.ACTIVIDAD, a.id());
            nodes.put(actNodeId, toActividadNode(a));
            addEdge(edges, rootId, actNodeId, EdgeType.OPORTUNIDAD_ACTIVIDAD, "Actividad", null);
        }

        // 7. Propuestas de la oportunidad
        List<PropuestaResponse> propuestas = propuestaUseCase.listarPorOportunidad(oportunidadId);
        for (PropuestaResponse p : propuestas) {
            String propNodeId = makeNodeId(NodeType.PROPUESTA, p.id());
            nodes.put(propNodeId, toPropuestaNode(p));
            addEdge(edges, rootId, propNodeId, EdgeType.OPORTUNIDAD_PROPUESTA, "Propuesta", null);
        }

        return new RelationshipMapResponse(
                rootId,
                new ArrayList<>(nodes.values()),
                new ArrayList<>(edges.values()),
                nodes.size(),
                edges.size()
        );
    }

    @Override
    public RelationshipMapResponse expandirNodo(NodeType tipo, Long id) {
        return switch (tipo) {
            case CLIENTE -> obtenerMapaCliente(id);
            case OPORTUNIDAD -> obtenerMapaOportunidad(id);
            case CONTACTO -> expandirContacto(id);
            case PROPUESTA -> expandirPropuesta(id);
            case ACTIVIDAD -> expandirActividad(id);
            case PRODUCTO -> expandirProducto(id);
            case USUARIO -> expandirUsuario(id);
        };
    }

    @Override
    public GraphNodeDto obtenerDetalleNodo(NodeType tipo, Long id) {
        return switch (tipo) {
            case CLIENTE -> toClienteNode(clienteUseCase.obtener(id), 0);
            case OPORTUNIDAD -> toOportunidadNode(oportunidadUseCase.obtener(id));
            case CONTACTO -> {
                Optional<ContactoResponse> opt = findContactoById(id);
                yield opt.map(this::toContactoNode).orElseThrow(() -> new ResourceNotFoundException("Contacto no encontrado: " + id));
            }
            case PROPUESTA -> toPropuestaNode(propuestaUseCase.obtener(id));
            case ACTIVIDAD -> toActividadNode(actividadUseCase.obtener(id));
            case PRODUCTO -> toProductoNode(productoUseCase.obtener(id));
            case USUARIO -> usuariosApi.buscarPorId(id)
                    .map(u -> new GraphNodeDto(
                            makeNodeId(NodeType.USUARIO, u.id()),
                            u.id(),
                            NodeType.USUARIO,
                            u.nombre() + " " + (u.apellido() != null ? u.apellido() : ""),
                            u.correo(),
                            u.estado() != null ? u.estado() : "ACTIVO",
                            u.rol() != null ? u.rol() : "USUARIO",
                            false,
                            0,
                            Map.of("correo", u.correo(), "rol", u.rol() != null ? u.rol() : "")
                    ))
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + id));
        };
    }

    private RelationshipMapResponse expandirContacto(Long contactoId) {
        Optional<ContactoResponse> opt = findContactoById(contactoId);
        if (opt.isEmpty()) {
            throw new ResourceNotFoundException("Contacto no encontrado: " + contactoId);
        }
        ContactoResponse contacto = opt.get();

        Map<String, GraphNodeDto> nodes = new LinkedHashMap<>();
        Map<String, GraphEdgeDto> edges = new LinkedHashMap<>();

        String rootId = makeNodeId(NodeType.CONTACTO, contacto.id());
        nodes.put(rootId, toContactoNode(contacto));

        // Cliente dueño
        if (contacto.idCliente() != null) {
            try {
                ClienteResponse cliente = clienteUseCase.obtener(contacto.idCliente());
                String cId = makeNodeId(NodeType.CLIENTE, cliente.id());
                nodes.put(cId, toClienteNode(cliente, 1));
                addEdge(edges, cId, rootId, EdgeType.CLIENTE_CONTACTO, "Pertenece a", null);
            } catch (Exception ignored) {}
        }

        // Oportunidades donde participa
        List<OportunidadResponse> allOpps = oportunidadUseCase.listar();
        for (OportunidadResponse o : allOpps) {
            List<OportunidadContactoResponse> conns = oportunidadUseCase.listarContactos(o.id());
            for (OportunidadContactoResponse oc : conns) {
                if (contactoId.equals(oc.contactoId())) {
                    String oppId = makeNodeId(NodeType.OPORTUNIDAD, o.id());
                    nodes.put(oppId, toOportunidadNode(o));
                    addEdge(edges, oppId, rootId, EdgeType.OPORTUNIDAD_CONTACTO,
                            Boolean.TRUE.equals(oc.esDecisor()) ? "Decisor" : "Participante", null);
                }
            }
        }

        // Actividades donde participa el contacto
        List<ActividadResponse> actividades = contacto.idCliente() != null
                ? actividadUseCase.filtrarPorCliente(contacto.idCliente())
                : actividadUseCase.listarTodos();
        for (ActividadResponse a : actividades) {
            List<ParticipanteResponse> parts = actividadUseCase.listarParticipantes(a.id());
            for (ParticipanteResponse pr : parts) {
                if (contactoId.equals(pr.contactoId())) {
                    String actId = makeNodeId(NodeType.ACTIVIDAD, a.id());
                    nodes.put(actId, toActividadNode(a));
                    addEdge(edges, actId, rootId, EdgeType.ACTIVIDAD_PARTICIPANTE, "Participante", null);
                    break;
                }
            }
        }

        return new RelationshipMapResponse(rootId, new ArrayList<>(nodes.values()), new ArrayList<>(edges.values()), nodes.size(), edges.size());
    }

    private RelationshipMapResponse expandirPropuesta(Long propuestaId) {
        PropuestaResponse p = propuestaUseCase.obtener(propuestaId);
        if (p == null) throw new ResourceNotFoundException("Propuesta no encontrada: " + propuestaId);

        Map<String, GraphNodeDto> nodes = new LinkedHashMap<>();
        Map<String, GraphEdgeDto> edges = new LinkedHashMap<>();

        String rootId = makeNodeId(NodeType.PROPUESTA, p.id());
        nodes.put(rootId, toPropuestaNode(p));

        // 1. Cliente
        if (p.idCliente() != null) {
            try {
                ClienteResponse cl = clienteUseCase.obtener(p.idCliente());
                String cId = makeNodeId(NodeType.CLIENTE, cl.id());
                nodes.put(cId, toClienteNode(cl, 1));
                addEdge(edges, cId, rootId, EdgeType.CLIENTE_PROPUESTA, "Cliente", null);
            } catch (Exception ignored) {}
        }

        // 2. Oportunidad
        if (p.idOportunidad() != null) {
            try {
                OportunidadResponse opp = oportunidadUseCase.obtener(p.idOportunidad());
                String oId = makeNodeId(NodeType.OPORTUNIDAD, opp.id());
                nodes.put(oId, toOportunidadNode(opp));
                addEdge(edges, oId, rootId, EdgeType.OPORTUNIDAD_PROPUESTA, "Oportunidad", null);
            } catch (Exception ignored) {}
        }

        // 3. Productos cotizados en la propuesta
        if (p.detalles() != null) {
            for (DetallePropuestaResponse d : p.detalles()) {
                if (d.productoId() != null) {
                    try {
                        ProductoResponse prod = productoUseCase.obtener(d.productoId());
                        String prId = makeNodeId(NodeType.PRODUCTO, prod.id());
                        nodes.put(prId, toProductoNode(prod));
                        String edgeLabel = (d.cantidad() != null ? d.cantidad() + " un." : "Cotizado")
                                + (d.subtotal() != null ? " ($" + d.subtotal() + ")" : "");
                        Map<String, Object> edgeData = new HashMap<>();
                        if (d.cantidad() != null) edgeData.put("cantidad", d.cantidad());
                        if (d.precioUnitario() != null) edgeData.put("precioUnitario", d.precioUnitario());
                        if (d.subtotal() != null) edgeData.put("subtotal", d.subtotal());
                        addEdge(edges, rootId, prId, EdgeType.PROPUESTA_PRODUCTO, edgeLabel, edgeData);
                    } catch (Exception ignored) {}
                }
            }
        }

        // 4. Usuario responsable
        if (p.responsable() != null && !p.responsable().isBlank()) {
            usuariosApi.buscarPorCorreo(p.responsable()).ifPresent(u -> {
                String uNodeId = makeNodeId(NodeType.USUARIO, u.id());
                nodes.put(uNodeId, new GraphNodeDto(
                        uNodeId, u.id(), NodeType.USUARIO,
                        u.nombre() + " " + (u.apellido() != null ? u.apellido() : ""),
                        u.correo(),
                        u.estado() != null ? u.estado() : "ACTIVO",
                        "Responsable",
                        false,
                        1,
                        Map.of("correo", u.correo(), "rol", u.rol() != null ? u.rol() : "USUARIO")
                ));
                addEdge(edges, rootId, uNodeId, EdgeType.OPORTUNIDAD_COLABORADOR, "Responsable", null);
            });
        }

        return new RelationshipMapResponse(rootId, new ArrayList<>(nodes.values()), new ArrayList<>(edges.values()), nodes.size(), edges.size());
    }

    private RelationshipMapResponse expandirActividad(Long actividadId) {
        ActividadResponse act = actividadUseCase.obtener(actividadId);
        if (act == null) throw new ResourceNotFoundException("Actividad no encontrada: " + actividadId);

        Map<String, GraphNodeDto> nodes = new LinkedHashMap<>();
        Map<String, GraphEdgeDto> edges = new LinkedHashMap<>();

        String rootId = makeNodeId(NodeType.ACTIVIDAD, act.id());
        nodes.put(rootId, toActividadNode(act));

        // 1. Cliente vinculado
        if (act.idCliente() != null) {
            try {
                ClienteResponse cl = clienteUseCase.obtener(act.idCliente());
                String cId = makeNodeId(NodeType.CLIENTE, cl.id());
                nodes.put(cId, toClienteNode(cl, 1));
                addEdge(edges, cId, rootId, EdgeType.CLIENTE_ACTIVIDAD, "Cliente", null);
            } catch (Exception ignored) {}
        }

        // 2. Oportunidad vinculada
        if (act.idOportunidad() != null) {
            try {
                OportunidadResponse opp = oportunidadUseCase.obtener(act.idOportunidad());
                String oId = makeNodeId(NodeType.OPORTUNIDAD, opp.id());
                nodes.put(oId, toOportunidadNode(opp));
                addEdge(edges, oId, rootId, EdgeType.OPORTUNIDAD_ACTIVIDAD, "Oportunidad", null);
            } catch (Exception ignored) {}
        }

        // 3. Contactos participantes externos
        List<ParticipanteResponse> parts = actividadUseCase.listarParticipantes(actividadId);
        for (ParticipanteResponse part : parts) {
            findContactoById(part.contactoId()).ifPresent(c -> {
                String cId = makeNodeId(NodeType.CONTACTO, c.id());
                nodes.put(cId, toContactoNode(c));
                addEdge(edges, rootId, cId, EdgeType.ACTIVIDAD_PARTICIPANTE, "Participante", null);
            });
        }

        // 4. Usuario organizador / responsable interno
        if (act.idUsuario() != null && !act.idUsuario().isBlank()) {
            Optional<UsuarioResumen> uOpt = Optional.empty();
            try {
                Long uId = Long.parseLong(act.idUsuario());
                uOpt = usuariosApi.buscarPorId(uId);
            } catch (NumberFormatException e) {
                uOpt = usuariosApi.buscarPorCorreo(act.idUsuario());
            }
            uOpt.ifPresent(u -> {
                String uNodeId = makeNodeId(NodeType.USUARIO, u.id());
                nodes.put(uNodeId, new GraphNodeDto(
                        uNodeId, u.id(), NodeType.USUARIO,
                        u.nombre() + " " + (u.apellido() != null ? u.apellido() : ""),
                        u.correo(),
                        u.estado() != null ? u.estado() : "ACTIVO",
                        "Organizador",
                        false,
                        1,
                        Map.of("correo", u.correo(), "rol", u.rol() != null ? u.rol() : "USUARIO")
                ));
                addEdge(edges, rootId, uNodeId, EdgeType.ACTIVIDAD_PARTICIPANTE, "Organizador", null);
            });
        }

        return new RelationshipMapResponse(rootId, new ArrayList<>(nodes.values()), new ArrayList<>(edges.values()), nodes.size(), edges.size());
    }

    private RelationshipMapResponse expandirProducto(Long productoId) {
        ProductoResponse prod = productoUseCase.obtener(productoId);
        if (prod == null) throw new ResourceNotFoundException("Producto no encontrado: " + productoId);

        Map<String, GraphNodeDto> nodes = new LinkedHashMap<>();
        Map<String, GraphEdgeDto> edges = new LinkedHashMap<>();

        String rootId = makeNodeId(NodeType.PRODUCTO, prod.id());
        nodes.put(rootId, toProductoNode(prod));

        // Buscar oportunidades donde se usa
        List<OportunidadResponse> allOpps = oportunidadUseCase.listar();
        for (OportunidadResponse o : allOpps) {
            List<OportunidadProductoResponse> oppProds = oportunidadUseCase.listarProductos(o.id());
            for (OportunidadProductoResponse op : oppProds) {
                if (productoId.equals(op.productoId())) {
                    String oppId = makeNodeId(NodeType.OPORTUNIDAD, o.id());
                    nodes.put(oppId, toOportunidadNode(o));
                    addEdge(edges, oppId, rootId, EdgeType.OPORTUNIDAD_PRODUCTO,
                            op.cantidad() != null ? op.cantidad() + " un." : "Incluye", null);
                }
            }
        }

        return new RelationshipMapResponse(rootId, new ArrayList<>(nodes.values()), new ArrayList<>(edges.values()), nodes.size(), edges.size());
    }

    private RelationshipMapResponse expandirUsuario(Long usuarioId) {
        UsuarioResumen u = usuariosApi.buscarPorId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + usuarioId));

        Map<String, GraphNodeDto> nodes = new LinkedHashMap<>();
        Map<String, GraphEdgeDto> edges = new LinkedHashMap<>();

        String rootId = makeNodeId(NodeType.USUARIO, u.id());
        nodes.put(rootId, new GraphNodeDto(
                rootId, u.id(), NodeType.USUARIO,
                u.nombre() + " " + (u.apellido() != null ? u.apellido() : ""),
                u.correo(), u.estado() != null ? u.estado() : "ACTIVO",
                u.rol() != null ? u.rol() : "USUARIO", false, 0,
                Map.of("correo", u.correo(), "rol", u.rol() != null ? u.rol() : "")
        ));

        // Buscar oportunidades donde colabora
        List<OportunidadResponse> allOpps = oportunidadUseCase.listar();
        for (OportunidadResponse o : allOpps) {
            List<OportunidadColaboradorResponse> colabs = oportunidadUseCase.listarColaboradores(o.id());
            for (OportunidadColaboradorResponse c : colabs) {
                if (usuarioId.equals(c.usuarioId())) {
                    String oppId = makeNodeId(NodeType.OPORTUNIDAD, o.id());
                    nodes.put(oppId, toOportunidadNode(o));
                    addEdge(edges, oppId, rootId, EdgeType.OPORTUNIDAD_COLABORADOR, "Colaborador", null);
                }
            }
        }

        return new RelationshipMapResponse(rootId, new ArrayList<>(nodes.values()), new ArrayList<>(edges.values()), nodes.size(), edges.size());
    }

    // --- Helpers de Mapeo de Nodos ---

    private GraphNodeDto toClienteNode(ClienteResponse c, int connCount) {
        Map<String, Object> data = new HashMap<>();
        if (c.empresa() != null) data.put("empresa", c.empresa());
        if (c.correo() != null) data.put("correo", c.correo());
        if (c.telefono() != null) data.put("telefono", c.telefono());
        if (c.responsable() != null) data.put("responsable", c.responsable());
        if (c.creadoEn() != null) data.put("creadoEn", c.creadoEn());

        return new GraphNodeDto(
                makeNodeId(NodeType.CLIENTE, c.id()),
                c.id(),
                NodeType.CLIENTE,
                c.nombre(),
                c.empresa() != null && !c.empresa().isBlank() ? c.empresa() : c.correo(),
                c.estado() != null ? c.estado().name() : "ACTIVO",
                "Cliente",
                true,
                connCount,
                data
        );
    }

    private GraphNodeDto toContactoNode(ContactoResponse c) {
        Map<String, Object> data = new HashMap<>();
        if (c.cargo() != null) data.put("cargo", c.cargo());
        if (c.correo() != null) data.put("correo", c.correo());
        if (c.telefono() != null) data.put("telefono", c.telefono());
        data.put("principal", Boolean.TRUE.equals(c.principal()));

        String fullName = c.nombre() + (c.apellido() != null && !c.apellido().isBlank() ? " " + c.apellido() : "");
        return new GraphNodeDto(
                makeNodeId(NodeType.CONTACTO, c.id()),
                c.id(),
                NodeType.CONTACTO,
                fullName,
                c.cargo() != null && !c.cargo().isBlank() ? c.cargo() : c.correo(),
                "ACTIVO",
                Boolean.TRUE.equals(c.principal()) ? "Principal" : "",
                true,
                1,
                data
        );
    }

    private GraphNodeDto toOportunidadNode(OportunidadResponse o) {
        Map<String, Object> data = new HashMap<>();
        if (o.valor() != null) data.put("valor", o.valor());
        if (o.probabilidad() != null) data.put("probabilidad", o.probabilidad());
        if (o.etapa() != null) data.put("etapa", o.etapa().name());
        if (o.responsable() != null) data.put("responsable", o.responsable());
        if (o.fechaCierre() != null) data.put("fechaCierre", o.fechaCierre().toString());

        String badge = o.valor() != null ? "$" + o.valor() : (o.etapa() != null ? o.etapa().name() : "");
        return new GraphNodeDto(
                makeNodeId(NodeType.OPORTUNIDAD, o.id()),
                o.id(),
                NodeType.OPORTUNIDAD,
                o.nombre(),
                o.etapa() != null ? o.etapa().name() : "Oportunidad",
                o.etapa() != null ? o.etapa().name() : "PROSPECCION",
                badge,
                true,
                1,
                data
        );
    }

    private GraphNodeDto toActividadNode(ActividadResponse a) {
        Map<String, Object> data = new HashMap<>();
        if (a.tipo() != null) data.put("tipo", a.tipo().name());
        if (a.estado() != null) data.put("estado", a.estado().name());
        if (a.fecha() != null) data.put("fecha", a.fecha().toString());
        if (a.descripcion() != null) data.put("descripcion", a.descripcion());
        data.put("vencida", a.vencida());
        if (a.etiquetas() != null && !a.etiquetas().isEmpty()) {
            data.put("etiquetasCount", a.etiquetas().size());
        }

        String tipoLabel = a.tipo() != null ? a.tipo().name() : "ACTIVIDAD";
        String estadoLabel = a.estado() != null ? a.estado().name() : "PENDIENTE";
        String badge = a.vencida() ? "¡VENCIDA!" : estadoLabel;

        return new GraphNodeDto(
                makeNodeId(NodeType.ACTIVIDAD, a.id()),
                a.id(),
                NodeType.ACTIVIDAD,
                a.titulo(),
                tipoLabel + " • " + estadoLabel,
                estadoLabel,
                badge,
                true,
                1,
                data
        );
    }

    private GraphNodeDto toPropuestaNode(PropuestaResponse p) {
        Map<String, Object> data = new HashMap<>();
        if (p.total() != null) data.put("total", p.total());
        if (p.subtotal() != null) data.put("subtotal", p.subtotal());
        if (p.descuento() != null) data.put("descuento", p.descuento());
        if (p.estado() != null) data.put("estado", p.estado().name());
        if (p.version() != null) data.put("version", p.version());
        if (p.fechaEmision() != null) data.put("fechaEmision", p.fechaEmision().toString());
        if (p.fechaVencimiento() != null) data.put("fechaVencimiento", p.fechaVencimiento().toString());
        if (p.responsable() != null) data.put("responsable", p.responsable());
        if (p.observaciones() != null) data.put("observaciones", p.observaciones());

        String badge = (p.total() != null ? "$" + p.total() : "") + (p.version() != null ? " (v" + p.version() + ")" : "");
        String sublabel = (p.estado() != null ? p.estado().name() : "Propuesta") + (p.version() != null ? " • v" + p.version() : "");

        return new GraphNodeDto(
                makeNodeId(NodeType.PROPUESTA, p.id()),
                p.id(),
                NodeType.PROPUESTA,
                p.numeroPropuesta() != null ? p.numeroPropuesta() : "Propuesta #" + p.id(),
                sublabel,
                p.estado() != null ? p.estado().name() : "BORRADOR",
                badge,
                true,
                1,
                data
        );
    }

    private GraphNodeDto toProductoNode(ProductoResponse p) {
        Map<String, Object> data = new HashMap<>();
        if (p.codigo() != null) data.put("codigo", p.codigo());
        if (p.precioBase() != null) data.put("precioBase", p.precioBase());
        if (p.categoria() != null) data.put("categoria", p.categoria());

        return new GraphNodeDto(
                makeNodeId(NodeType.PRODUCTO, p.id()),
                p.id(),
                NodeType.PRODUCTO,
                p.nombre(),
                p.codigo() != null ? "Cód: " + p.codigo() : "",
                p.estado() != null ? p.estado().name() : "ACTIVO",
                p.precioBase() != null ? "$" + p.precioBase() : "",
                false,
                1,
                data
        );
    }

    // --- Métodos de Búsqueda y Resolución Cruzada ---

    private List<OportunidadResponse> findOportunidadesPorCliente(ClienteResponse cliente, List<ContactoResponse> contactos) {
        Set<Long> contactoIds = new HashSet<>();
        for (ContactoResponse c : contactos) {
            contactoIds.add(c.id());
        }

        List<OportunidadResponse> todas = oportunidadUseCase.listar();
        List<OportunidadResponse> result = new ArrayList<>();

        for (OportunidadResponse o : todas) {
            // Coincidencia por nombre denormalizado
            if (o.clienteNombre() != null && (
                    o.clienteNombre().equalsIgnoreCase(cliente.nombre()) ||
                    (cliente.empresa() != null && o.clienteNombre().equalsIgnoreCase(cliente.empresa()))
            )) {
                result.add(o);
                continue;
            }

            // Coincidencia por contactos enlazados
            List<OportunidadContactoResponse> oppConns = oportunidadUseCase.listarContactos(o.id());
            for (OportunidadContactoResponse oc : oppConns) {
                if (contactoIds.contains(oc.contactoId())) {
                    result.add(o);
                    break;
                }
            }
        }
        return result;
    }

    private ClienteResponse findClientePorNombre(String clienteNombre) {
        if (clienteNombre == null || clienteNombre.isBlank()) return null;
        List<ClienteResponse> matches = clienteUseCase.buscar(clienteNombre);
        for (ClienteResponse c : matches) {
            if (clienteNombre.equalsIgnoreCase(c.nombre()) ||
                (c.empresa() != null && clienteNombre.equalsIgnoreCase(c.empresa()))) {
                return c;
            }
        }
        return matches.isEmpty() ? null : matches.get(0);
    }

    private Optional<ContactoResponse> findContactoById(Long contactoId) {
        List<ClienteResponse> clientes = clienteUseCase.listar();
        for (ClienteResponse cl : clientes) {
            List<ContactoResponse> contactos = clienteUseCase.listarContactos(cl.id());
            for (ContactoResponse c : contactos) {
                if (contactoId.equals(c.id())) {
                    return Optional.of(c);
                }
            }
        }
        return Optional.empty();
    }

    private String makeNodeId(NodeType type, Long id) {
        return type.name() + ":" + id;
    }

    private void addEdge(Map<String, GraphEdgeDto> edges, String source, String target, EdgeType type, String label, Map<String, Object> data) {
        String edgeId = source + "->" + target;
        edges.put(edgeId, new GraphEdgeDto(edgeId, source, target, type, label, data != null ? data : Collections.emptyMap()));
    }
}
