package com.viernes.clientes.application.service;

import com.viernes.clientes.application.dto.ClienteRequest;
import com.viernes.clientes.application.dto.ClienteResponse;
import com.viernes.clientes.application.dto.ContactoRequest;
import com.viernes.clientes.application.dto.ContactoResponse;
import com.viernes.clientes.application.dto.DocumentoConsultaResponse;
import com.viernes.clientes.application.port.out.ClientePersistencePort;
import com.viernes.clientes.application.port.out.DocumentoLookupPort;
import com.viernes.clientes.domain.Cliente;
import com.viernes.clientes.domain.Contacto;
import com.viernes.clientes.domain.DocumentoConsultado;
import com.viernes.clientes.domain.EstadoCliente;
import com.viernes.clientes.domain.TipoDocumento;
import com.viernes.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClientePersistencePort persistencePort;

    @Mock
    private DocumentoLookupPort documentoLookupPort;

    @InjectMocks
    private ClienteService servicio;

    private ClienteRequest validRequest;
    private ContactoRequest contactoRequest;
    private Cliente entidadGuardada;

    @BeforeEach
    void setUp() {
        validRequest = new ClienteRequest(
                "Juan Perez", "Empresa SA", TipoDocumento.DNI, "12345678", "juan@mail.com", "555-1234", null, "Maria"
        );

        contactoRequest = new ContactoRequest(
                "Ana", "Lopez", "Gerente", "ana@mail.com", "555-1111", null
        );

        entidadGuardada = Cliente.builder()
                .id(1L)
                .nombre("Juan Perez")
                .empresa("Empresa SA")
                .tipoDocumento(TipoDocumento.DNI)
                .numeroDocumento("12345678")
                .correo("juan@mail.com")
                .telefono("555-1234")
                .estado(EstadoCliente.ACTIVO)
                .responsable("Maria")
                .creadoEn(LocalDateTime.now())
                .contactos(new ArrayList<>())
                .build();
    }

    @Test
    void listar_retornaTodosLosClientes() {
        when(persistencePort.findAll()).thenReturn(List.of(entidadGuardada));

        List<ClienteResponse> resultado = servicio.listar();

        assertEquals(1, resultado.size());
        assertEquals("Juan Perez", resultado.get(0).nombre());
    }

    @Test
    void buscar_conTexto_retornaCoincidencias() {
        when(persistencePort.findByNombreOrEmpresa("juan")).thenReturn(List.of(entidadGuardada));

        List<ClienteResponse> resultado = servicio.buscar("juan");

        assertEquals(1, resultado.size());
        assertEquals(1L, resultado.get(0).id());
    }

    @Test
    void buscarPorDocumento_retornaCoincidencias() {
        when(persistencePort.findByNumeroDocumento("12345678")).thenReturn(List.of(entidadGuardada));

        List<ClienteResponse> resultado = servicio.buscarPorDocumento("12345678");

        assertEquals(1, resultado.size());
        assertEquals(TipoDocumento.DNI, resultado.get(0).tipoDocumento());
        assertEquals("12345678", resultado.get(0).numeroDocumento());
    }

    @Test
    void consultarDocumento_dni_retornaDatosOficiales() {
        DocumentoConsultado documento = new DocumentoConsultado(
                TipoDocumento.DNI,
                "12345678",
                "Juan Perez Lopez",
                null,
                null,
                null,
                null,
                null,
                List.of()
        );
        when(documentoLookupPort.consultar(TipoDocumento.DNI, "12345678")).thenReturn(documento);

        DocumentoConsultaResponse response = servicio.consultarDocumento(TipoDocumento.DNI, " 12345678 ");

        assertEquals(TipoDocumento.DNI, response.tipoDocumento());
        assertEquals("12345678", response.numeroDocumento());
        assertEquals("Juan Perez Lopez", response.nombreCompleto());
        verify(documentoLookupPort).consultar(TipoDocumento.DNI, "12345678");
    }

    @Test
    void consultarDocumento_ruc_retornaDatosOficiales() {
        DocumentoConsultado documento = new DocumentoConsultado(
                TipoDocumento.RUC,
                "20123456789",
                null,
                "Empresa SAC",
                "Empresa",
                "ACTIVO",
                "HABIDO",
                "Av. Lima 123",
                List.of("01-1234567")
        );
        when(documentoLookupPort.consultar(TipoDocumento.RUC, "20123456789")).thenReturn(documento);

        DocumentoConsultaResponse response = servicio.consultarDocumento(TipoDocumento.RUC, "20123456789");

        assertEquals(TipoDocumento.RUC, response.tipoDocumento());
        assertEquals("Empresa SAC", response.razonSocial());
        assertEquals("ACTIVO", response.estado());
        assertEquals("HABIDO", response.condicion());
        assertEquals("Av. Lima 123", response.direccion());
        assertEquals(List.of("01-1234567"), response.telefonos());
    }

    @Test
    void consultarDocumento_conNumeroInvalido_lanzaIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.consultarDocumento(TipoDocumento.DNI, "123"));
        verifyNoInteractions(documentoLookupPort);
    }

    @Test
    void filtrarPorEstado_retornaClientesDelEstado() {
        when(persistencePort.findByEstado(EstadoCliente.ACTIVO)).thenReturn(List.of(entidadGuardada));

        List<ClienteResponse> resultado = servicio.filtrarPorEstado(EstadoCliente.ACTIVO);

        assertEquals(1, resultado.size());
        assertEquals(EstadoCliente.ACTIVO, resultado.get(0).estado());
    }

    @Test
    void obtener_clienteExistente_retornaResponse() {
        when(persistencePort.findById(1L)).thenReturn(Optional.of(entidadGuardada));

        ClienteResponse response = servicio.obtener(1L);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Juan Perez", response.nombre());
        assertTrue(response.contactos().isEmpty());
    }

    @Test
    void obtener_clienteInexistente_lanzaNotFound() {
        when(persistencePort.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> servicio.obtener(99L));
    }

    @Test
    void crear_conDatosValidos_asignaEstadoActivoYCreadoEn() {
        when(persistencePort.save(any(Cliente.class))).thenAnswer(i -> i.getArgument(0));

        ClienteResponse response = servicio.crear(validRequest);

        assertNotNull(response);
        assertEquals(EstadoCliente.ACTIVO, response.estado());
        assertNotNull(response.creadoEn());
        assertEquals("Juan Perez", response.nombre());
        assertEquals("Empresa SA", response.empresa());
        assertEquals(TipoDocumento.DNI, response.tipoDocumento());
        assertEquals("12345678", response.numeroDocumento());
    }

    @Test
    void crear_conDocumentoDuplicado_lanzaIllegalArgument() {
        when(persistencePort.existsByNumeroDocumento("12345678")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> servicio.crear(validRequest));
        verify(persistencePort, never()).save(any());
    }

    @Test
    void crear_conDniInvalido_lanzaIllegalArgument() {
        ClienteRequest invalido = new ClienteRequest(
                "Juan Perez", "Empresa SA", TipoDocumento.DNI, "1234",
                "juan@mail.com", "555-1234", null, "Maria"
        );

        assertThrows(IllegalArgumentException.class, () -> servicio.crear(invalido));
        verify(persistencePort, never()).save(any());
    }

    @Test
    void crear_conRucInvalido_lanzaIllegalArgument() {
        ClienteRequest invalido = new ClienteRequest(
                "Empresa SA", "Empresa SA", TipoDocumento.RUC, "12345678",
                "empresa@mail.com", "555-1234", null, "Maria"
        );

        assertThrows(IllegalArgumentException.class, () -> servicio.crear(invalido));
        verify(persistencePort, never()).save(any());
    }

    @Test
    void actualizar_clienteExistente_retornaActualizado() {
        ClienteRequest cambios = new ClienteRequest(
                "Juan Actualizado", "Nueva Empresa", TipoDocumento.DNI, "87654321",
                "nuevo@mail.com", "555-9999", EstadoCliente.INACTIVO, "Pedro"
        );
        when(persistencePort.findById(1L)).thenReturn(Optional.of(entidadGuardada));
        when(persistencePort.save(any(Cliente.class))).thenAnswer(i -> i.getArgument(0));

        ClienteResponse response = servicio.actualizar(1L, cambios);

        assertNotNull(response);
        assertEquals("Juan Actualizado", response.nombre());
        assertEquals("Nueva Empresa", response.empresa());
        assertEquals(EstadoCliente.INACTIVO, response.estado());
        verify(persistencePort).save(any(Cliente.class));
    }

    @Test
    void actualizar_clienteInexistente_lanzaNotFound() {
        when(persistencePort.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> servicio.actualizar(99L, validRequest));
        verify(persistencePort, never()).save(any());
    }

    @Test
    void desactivar_clienteExistente_cambiaEstadoAInactivo() {
        when(persistencePort.findById(1L)).thenReturn(Optional.of(entidadGuardada));
        when(persistencePort.save(any(Cliente.class))).thenAnswer(i -> i.getArgument(0));

        servicio.desactivar(1L);

        assertEquals(EstadoCliente.INACTIVO, entidadGuardada.getEstado());
        verify(persistencePort).save(any(Cliente.class));
    }

    @Test
    void desactivar_clienteInexistente_lanzaNotFound() {
        when(persistencePort.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> servicio.desactivar(99L));
        verify(persistencePort, never()).save(any());
    }

    @Test
    void eliminar_clienteExistente_eliminaCorrectamente() {
        when(persistencePort.findById(1L)).thenReturn(Optional.of(entidadGuardada));

        servicio.eliminar(1L);

        verify(persistencePort).deleteById(1L);
    }

    @Test
    void eliminar_clienteInexistente_lanzaNotFound() {
        when(persistencePort.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> servicio.eliminar(99L));
        verify(persistencePort, never()).deleteById(any());
    }

    @Test
    void agregarContacto_clienteExistente_retornaContacto() {
        when(persistencePort.findById(1L)).thenReturn(Optional.of(entidadGuardada));
        when(persistencePort.save(any(Cliente.class))).thenAnswer(i -> i.getArgument(0));

        ContactoResponse response = servicio.agregarContacto(1L, contactoRequest);

        assertNotNull(response);
        assertEquals("Ana", response.nombre());
        assertEquals(1L, response.idCliente());
        assertFalse(response.principal());
        assertNotNull(response.creadoEn());
    }

    @Test
    void agregarContacto_clienteInexistente_lanzaNotFound() {
        when(persistencePort.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> servicio.agregarContacto(99L, contactoRequest));
        verify(persistencePort, never()).save(any());
    }

    @Test
    void actualizarContacto_contactoExistente_retornaActualizado() {
        Contacto existente = Contacto.builder()
                .id(10L)
                .idCliente(1L)
                .nombre("Ana")
                .apellido("Lopez")
                .cargo("Gerente")
                .correo("ana@mail.com")
                .telefono("555-1111")
                .principal(false)
                .creadoEn(LocalDateTime.now())
                .build();
        entidadGuardada.getContactos().add(existente);
        ContactoRequest cambios = new ContactoRequest(
                "Ana Maria", "Lopez", "Directora", "anamaria@mail.com", "555-2222", true
        );
        when(persistencePort.findById(1L)).thenReturn(Optional.of(entidadGuardada));
        when(persistencePort.save(any(Cliente.class))).thenAnswer(i -> i.getArgument(0));

        ContactoResponse response = servicio.actualizarContacto(1L, 10L, cambios);

        assertNotNull(response);
        assertEquals("Ana Maria", response.nombre());
        assertEquals("Directora", response.cargo());
        assertTrue(response.principal());
        verify(persistencePort).save(any(Cliente.class));
    }

    @Test
    void actualizarContacto_contactoInexistente_lanzaNotFound() {
        when(persistencePort.findById(1L)).thenReturn(Optional.of(entidadGuardada));

        assertThrows(ResourceNotFoundException.class, () -> servicio.actualizarContacto(1L, 99L, contactoRequest));
        verify(persistencePort, never()).save(any());
    }

    @Test
    void eliminarContacto_contactoExistente_eliminaCorrectamente() {
        Contacto existente = Contacto.builder()
                .id(10L)
                .idCliente(1L)
                .nombre("Ana")
                .apellido("Lopez")
                .cargo("Gerente")
                .correo("ana@mail.com")
                .telefono("555-1111")
                .principal(false)
                .creadoEn(LocalDateTime.now())
                .build();
        entidadGuardada.getContactos().add(existente);
        when(persistencePort.findById(1L)).thenReturn(Optional.of(entidadGuardada));
        when(persistencePort.save(any(Cliente.class))).thenAnswer(i -> i.getArgument(0));

        servicio.eliminarContacto(1L, 10L);

        assertTrue(entidadGuardada.getContactos().isEmpty());
        verify(persistencePort).save(any(Cliente.class));
    }

    @Test
    void eliminarContacto_contactoInexistente_lanzaNotFound() {
        when(persistencePort.findById(1L)).thenReturn(Optional.of(entidadGuardada));

        assertThrows(ResourceNotFoundException.class, () -> servicio.eliminarContacto(1L, 99L));
        verify(persistencePort, never()).save(any());
    }
}
