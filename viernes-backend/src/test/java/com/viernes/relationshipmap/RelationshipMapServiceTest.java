package com.viernes.relationshipmap;

import com.viernes.actividades.application.dto.ActividadResponse;
import com.viernes.actividades.application.port.in.ManageActividadUseCase;
import com.viernes.actividades.domain.EstadoActividad;
import com.viernes.actividades.domain.TipoActividad;
import com.viernes.clientes.application.dto.ClienteResponse;
import com.viernes.clientes.application.dto.ContactoResponse;
import com.viernes.clientes.application.port.in.ManageClienteUseCase;
import com.viernes.clientes.domain.EstadoCliente;
import com.viernes.clientes.domain.TipoDocumento;
import com.viernes.oportunidades.application.dto.OportunidadResponse;
import com.viernes.oportunidades.application.port.in.ManageOportunidadUseCase;
import com.viernes.oportunidades.domain.EtapaOportunidad;
import com.viernes.productos.application.port.in.ManageProductoUseCase;
import com.viernes.propuestas.application.dto.PropuestaResponse;
import com.viernes.propuestas.application.port.in.ManagePropuestaUseCase;
import com.viernes.propuestas.domain.EstadoPropuesta;
import com.viernes.relationshipmap.application.dto.GraphNodeDto;
import com.viernes.relationshipmap.application.dto.RelationshipMapResponse;
import com.viernes.relationshipmap.application.service.RelationshipMapService;
import com.viernes.relationshipmap.domain.NodeType;
import com.viernes.shared.exception.ResourceNotFoundException;
import com.viernes.usuarios.UsuariosApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RelationshipMapServiceTest {

    @Mock
    private ManageClienteUseCase clienteUseCase;

    @Mock
    private ManageOportunidadUseCase oportunidadUseCase;

    @Mock
    private ManageActividadUseCase actividadUseCase;

    @Mock
    private ManagePropuestaUseCase propuestaUseCase;

    @Mock
    private ManageProductoUseCase productoUseCase;

    @Mock
    private UsuariosApi usuariosApi;

    @InjectMocks
    private RelationshipMapService relationshipMapService;

    private ClienteResponse clienteMock;
    private ContactoResponse contactoMock;
    private OportunidadResponse oportunidadMock;
    private ActividadResponse actividadMock;
    private PropuestaResponse propuestaMock;

    @BeforeEach
    void setUp() {
        clienteMock = new ClienteResponse(
                1L, "ACME Corp", "ACME Corporation", TipoDocumento.RUC, "20123456789",
                "contact@acme.com", "+51999999999",
                EstadoCliente.ACTIVO, "Juan Perez", LocalDateTime.now(), List.of()
        );

        contactoMock = new ContactoResponse(
                10L, 1L, "Carlos", "Mendoza", "Gerente TI", "carlos@acme.com", "+51988888888",
                true, LocalDateTime.now()
        );

        oportunidadMock = new OportunidadResponse(
                100L, "Renovación Servidores", "ACME Corp", BigDecimal.valueOf(25000), 80,
                BigDecimal.valueOf(20000), EtapaOportunidad.PROPUESTA, "Juan Perez",
                LocalDate.now().plusDays(30), "Migración y soporte anual", LocalDateTime.now()
        );

        actividadMock = new ActividadResponse(
                50L, "admin", 1L, null, null, TipoActividad.REUNION, "Revisión técnica",
                "Presentación de la propuesta de arquitectura", LocalDateTime.now(),
                EstadoActividad.PENDIENTE, LocalDateTime.now(), false, List.of()
        );

        propuestaMock = new PropuestaResponse(
                200L, "PROP-2026-001", 100L, 1L, "ACME Corp", EstadoPropuesta.ENVIADA,
                1, BigDecimal.valueOf(25000), BigDecimal.ZERO, BigDecimal.valueOf(25000),
                "Juan Perez", LocalDate.now(), LocalDate.now().plusDays(15), "Válida por 15 días",
                LocalDateTime.now(), List.of()
        );
    }

    @Test
    void obtenerMapaCliente_Exitoso() {
        when(clienteUseCase.obtener(1L)).thenReturn(clienteMock);
        when(clienteUseCase.listarContactos(1L)).thenReturn(List.of(contactoMock));
        when(oportunidadUseCase.listar()).thenReturn(List.of(oportunidadMock));
        when(actividadUseCase.filtrarPorCliente(1L)).thenReturn(List.of(actividadMock));
        when(propuestaUseCase.listarPorCliente(1L)).thenReturn(List.of(propuestaMock));

        RelationshipMapResponse response = relationshipMapService.obtenerMapaCliente(1L);

        assertNotNull(response);
        assertEquals("CLIENTE:1", response.rootNodeId());
        assertEquals(5, response.totalNodes());
        assertEquals(4, response.totalEdges());

        assertTrue(response.nodes().stream().anyMatch(n -> n.id().equals("CLIENTE:1") && n.type() == NodeType.CLIENTE));
        assertTrue(response.nodes().stream().anyMatch(n -> n.id().equals("CONTACTO:10") && n.type() == NodeType.CONTACTO));
        assertTrue(response.nodes().stream().anyMatch(n -> n.id().equals("OPORTUNIDAD:100") && n.type() == NodeType.OPORTUNIDAD));
        assertTrue(response.nodes().stream().anyMatch(n -> n.id().equals("ACTIVIDAD:50") && n.type() == NodeType.ACTIVIDAD));
        assertTrue(response.nodes().stream().anyMatch(n -> n.id().equals("PROPUESTA:200") && n.type() == NodeType.PROPUESTA));
    }

    @Test
    void obtenerMapaCliente_NoEncontrado_LanzaExcepcion() {
        when(clienteUseCase.obtener(999L)).thenThrow(new ResourceNotFoundException("Cliente no encontrado con id: 999"));

        assertThrows(ResourceNotFoundException.class, () -> relationshipMapService.obtenerMapaCliente(999L));
    }

    @Test
    void obtenerDetalleNodo_Cliente_Exitoso() {
        when(clienteUseCase.obtener(1L)).thenReturn(clienteMock);

        GraphNodeDto node = relationshipMapService.obtenerDetalleNodo(NodeType.CLIENTE, 1L);

        assertNotNull(node);
        assertEquals("CLIENTE:1", node.id());
        assertEquals("ACME Corp", node.label());
        assertEquals(NodeType.CLIENTE, node.type());
    }

    @Test
    void expandirNodo_Contacto_Exitoso() {
        when(clienteUseCase.listar()).thenReturn(List.of(clienteMock));
        when(clienteUseCase.listarContactos(1L)).thenReturn(List.of(contactoMock));
        when(clienteUseCase.obtener(1L)).thenReturn(clienteMock);
        when(oportunidadUseCase.listar()).thenReturn(List.of(oportunidadMock));
        when(oportunidadUseCase.listarContactos(100L)).thenReturn(List.of(
                new com.viernes.oportunidades.application.dto.OportunidadContactoResponse(
                        1L, 100L, 10L, "Carlos Mendoza", "carlos@acme.com", "Decisor", true, LocalDateTime.now()
                )
        ));
        when(actividadUseCase.filtrarPorCliente(1L)).thenReturn(List.of(actividadMock));
        when(actividadUseCase.listarParticipantes(50L)).thenReturn(List.of(
                new com.viernes.actividades.application.dto.ParticipanteResponse(
                        1L, 50L, 10L, "Carlos Mendoza", "carlos@acme.com", LocalDateTime.now()
                )
        ));

        RelationshipMapResponse response = relationshipMapService.expandirNodo(NodeType.CONTACTO, 10L);

        assertNotNull(response);
        assertEquals("CONTACTO:10", response.rootNodeId());
        assertTrue(response.nodes().stream().anyMatch(n -> n.id().equals("CLIENTE:1")));
        assertTrue(response.nodes().stream().anyMatch(n -> n.id().equals("OPORTUNIDAD:100")));
        assertTrue(response.nodes().stream().anyMatch(n -> n.id().equals("ACTIVIDAD:50")));
    }

    @Test
    void obtenerMapaOportunidad_Exitoso() {
        when(oportunidadUseCase.obtener(100L)).thenReturn(oportunidadMock);
        when(oportunidadUseCase.listarContactos(100L)).thenReturn(List.of(
                new com.viernes.oportunidades.application.dto.OportunidadContactoResponse(
                        1L, 100L, 10L, "Carlos Mendoza", "carlos@acme.com", "Decisor", true, LocalDateTime.now()
                )
        ));
        when(clienteUseCase.buscar("ACME Corp")).thenReturn(List.of(clienteMock));
        when(oportunidadUseCase.listarProductos(100L)).thenReturn(List.of());
        when(oportunidadUseCase.listarColaboradores(100L)).thenReturn(List.of());
        when(actividadUseCase.filtrarPorOportunidad(100L)).thenReturn(List.of());
        when(propuestaUseCase.listarPorOportunidad(100L)).thenReturn(List.of());

        RelationshipMapResponse response = relationshipMapService.obtenerMapaOportunidad(100L);

        assertNotNull(response);
        assertEquals("OPORTUNIDAD:100", response.rootNodeId());
        assertTrue(response.nodes().stream().anyMatch(n -> n.id().equals("OPORTUNIDAD:100")));
        assertTrue(response.nodes().stream().anyMatch(n -> n.id().equals("CLIENTE:1")));
        assertTrue(response.nodes().stream().anyMatch(n -> n.id().equals("CONTACTO:10")));
    }

    @Test
    void expandirNodo_Propuesta_Exitoso() {
        com.viernes.productos.application.dto.ProductoResponse productoMock =
                new com.viernes.productos.application.dto.ProductoResponse(
                        30L, "PROD-01", "Servidor Cloud", "Servidor Cloud", "Servicios",
                        BigDecimal.valueOf(1000), com.viernes.productos.domain.EstadoProducto.ACTIVO, LocalDateTime.now()
                );
        PropuestaResponse propuestaConDetalles = new PropuestaResponse(
                200L, "PROP-2026-001", 100L, 1L, "ACME Corp", EstadoPropuesta.APROBADA,
                1, BigDecimal.valueOf(25000), BigDecimal.ZERO, BigDecimal.valueOf(25000),
                "juan@viernes.com", LocalDate.now(), LocalDate.now().plusDays(15), "Aprobada",
                LocalDateTime.now(), List.of(
                        new com.viernes.propuestas.application.dto.DetallePropuestaResponse(
                                1L, 30L, "Servidor Cloud Anual", 2, BigDecimal.valueOf(1000), BigDecimal.valueOf(2000)
                        )
                )
        );

        when(propuestaUseCase.obtener(200L)).thenReturn(propuestaConDetalles);
        when(clienteUseCase.obtener(1L)).thenReturn(clienteMock);
        when(oportunidadUseCase.obtener(100L)).thenReturn(oportunidadMock);
        when(productoUseCase.obtener(30L)).thenReturn(productoMock);
        when(usuariosApi.buscarPorCorreo("juan@viernes.com")).thenReturn(java.util.Optional.of(
                new com.viernes.usuarios.UsuarioResumen(99L, "Juan", "Perez", "juan@viernes.com", "hash", "ADMIN", "ACTIVO", LocalDateTime.now())
        ));

        RelationshipMapResponse response = relationshipMapService.expandirNodo(NodeType.PROPUESTA, 200L);

        assertNotNull(response);
        assertEquals("PROPUESTA:200", response.rootNodeId());
        assertTrue(response.nodes().stream().anyMatch(n -> n.id().equals("PROPUESTA:200")));
        assertTrue(response.nodes().stream().anyMatch(n -> n.id().equals("CLIENTE:1")));
        assertTrue(response.nodes().stream().anyMatch(n -> n.id().equals("OPORTUNIDAD:100")));
        assertTrue(response.nodes().stream().anyMatch(n -> n.id().equals("PRODUCTO:30")));
        assertTrue(response.nodes().stream().anyMatch(n -> n.id().equals("USUARIO:99")));
    }

    @Test
    void expandirNodo_Actividad_Exitoso() {
        ActividadResponse actCompleta = new ActividadResponse(
                50L, "99", 1L, null, 100L, TipoActividad.REUNION, "Revisión técnica",
                "Presentación de la propuesta de arquitectura", LocalDateTime.now(),
                EstadoActividad.COMPLETADA, LocalDateTime.now(), false, List.of()
        );

        when(actividadUseCase.obtener(50L)).thenReturn(actCompleta);
        when(clienteUseCase.obtener(1L)).thenReturn(clienteMock);
        when(oportunidadUseCase.obtener(100L)).thenReturn(oportunidadMock);
        when(actividadUseCase.listarParticipantes(50L)).thenReturn(List.of(
                new com.viernes.actividades.application.dto.ParticipanteResponse(
                        1L, 50L, 10L, "Carlos Mendoza", "carlos@acme.com", LocalDateTime.now()
                )
        ));
        when(clienteUseCase.listar()).thenReturn(List.of(clienteMock));
        when(clienteUseCase.listarContactos(1L)).thenReturn(List.of(contactoMock));
        when(usuariosApi.buscarPorId(99L)).thenReturn(java.util.Optional.of(
                new com.viernes.usuarios.UsuarioResumen(99L, "Juan", "Perez", "juan@viernes.com", "hash", "ADMIN", "ACTIVO", LocalDateTime.now())
        ));

        RelationshipMapResponse response = relationshipMapService.expandirNodo(NodeType.ACTIVIDAD, 50L);

        assertNotNull(response);
        assertEquals("ACTIVIDAD:50", response.rootNodeId());
        assertTrue(response.nodes().stream().anyMatch(n -> n.id().equals("ACTIVIDAD:50")));
        assertTrue(response.nodes().stream().anyMatch(n -> n.id().equals("CLIENTE:1")));
        assertTrue(response.nodes().stream().anyMatch(n -> n.id().equals("OPORTUNIDAD:100")));
        assertTrue(response.nodes().stream().anyMatch(n -> n.id().equals("CONTACTO:10")));
        assertTrue(response.nodes().stream().anyMatch(n -> n.id().equals("USUARIO:99")));
    }
}
