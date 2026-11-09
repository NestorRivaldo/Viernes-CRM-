package com.viernes.propuestas.application.service;

import com.viernes.propuestas.application.dto.DetallePropuestaRequest;
import com.viernes.propuestas.application.dto.PropuestaRequest;
import com.viernes.propuestas.application.dto.PropuestaResponse;
import com.viernes.propuestas.application.port.out.PropuestaPersistencePort;
import com.viernes.propuestas.application.port.out.PropuestaRelacionValidationPort;
import com.viernes.propuestas.domain.DetallePropuesta;
import com.viernes.propuestas.domain.EstadoPropuesta;
import com.viernes.propuestas.domain.Propuesta;
import com.viernes.productos.application.dto.ProductoResponse;
import com.viernes.productos.application.port.in.ManageProductoUseCase;
import com.viernes.productos.domain.EstadoProducto;
import com.viernes.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PropuestaServiceTest {

    @Mock
    private PropuestaPersistencePort persistencePort;

    @Mock
    private PropuestaRelacionValidationPort validationPort;

    @Mock
    private ManageProductoUseCase productoUseCase;

    @InjectMocks
    private PropuestaService servicio;

    private PropuestaRequest validRequest;
    private Propuesta entidadGuardada;
    private ProductoResponse productoCatalogo;

    @BeforeEach
    void setUp() {
        DetallePropuestaRequest detalleReq = new DetallePropuestaRequest("Servicio", 2, new BigDecimal("100.00"), null);
        validRequest = new PropuestaRequest(
                1L, 1L, "Cliente XYZ", "Juan", LocalDate.now(), LocalDate.now().plusDays(15),
                new BigDecimal("10.00"), "Obs", List.of(detalleReq)
        );

        productoCatalogo = new ProductoResponse(
                5L, "COD-05", "Producto Catálogo", "Descripción de catálogo", "Categoría",
                new BigDecimal("250.00"), EstadoProducto.ACTIVO, LocalDateTime.now()
        );

        DetallePropuesta detalle = DetallePropuesta.builder()
                .id(1L)
                .descripcion("Servicio")
                .cantidad(2)
                .precioUnitario(new BigDecimal("100.00"))
                .subtotal(new BigDecimal("200.00"))
                .build();

        entidadGuardada = Propuesta.builder()
                .id(1L)
                .numeroPropuesta("PROP-2026-0001")
                .idOportunidad(1L)
                .idCliente(1L)
                .clienteNombre("Cliente XYZ")
                .estado(EstadoPropuesta.BORRADOR)
                .version(1)
                .subtotal(new BigDecimal("200.00"))
                .descuento(new BigDecimal("10.00"))
                .total(new BigDecimal("190.00"))
                .responsable("Juan")
                .fechaEmision(LocalDate.now())
                .fechaVencimiento(LocalDate.now().plusDays(15))
                .observaciones("Obs")
                .creadoEn(LocalDateTime.now())
                .detalles(List.of(detalle))
                .build();
    }

    @Test
    void crear_conDatosValidos_retornaResponse() {
        when(validationPort.existeOportunidad(1L)).thenReturn(true);
        when(validationPort.existeCliente(1L)).thenReturn(true);
        when(persistencePort.countByNumeroPropuestaStartsWith(anyString())).thenReturn(0L);
        when(persistencePort.save(any(Propuesta.class))).thenReturn(entidadGuardada);

        PropuestaResponse response = servicio.crear(validRequest);

        assertNotNull(response);
        assertEquals("PROP-2026-0001", response.numeroPropuesta());
        assertEquals(EstadoPropuesta.BORRADOR, response.estado());
        assertEquals(new BigDecimal("190.00"), response.total());
        assertEquals(1, response.detalles().size());
    }

    @Test
    void crear_oportunidadInexistente_lanzaNotFound() {
        when(validationPort.existeOportunidad(1L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> servicio.crear(validRequest));
        verify(persistencePort, never()).save(any());
    }

    @Test
    void actualizar_enBorrador_exitoso() {
        when(persistencePort.findById(1L)).thenReturn(Optional.of(entidadGuardada));
        when(validationPort.existeOportunidad(1L)).thenReturn(true);
        when(validationPort.existeCliente(1L)).thenReturn(true);
        when(persistencePort.save(any(Propuesta.class))).thenReturn(entidadGuardada);

        PropuestaResponse response = servicio.actualizar(1L, validRequest);

        assertNotNull(response);
        verify(persistencePort).save(any(Propuesta.class));
    }

    @Test
    void actualizar_enEnviada_lanzaConflict() {
        entidadGuardada.setEstado(EstadoPropuesta.ENVIADA);
        when(persistencePort.findById(1L)).thenReturn(Optional.of(entidadGuardada));

        assertThrows(IllegalStateException.class, () -> servicio.actualizar(1L, validRequest));
        verify(persistencePort, never()).save(any());
    }

    @Test
    void enviar_desdeBorrador_cambiaEstado() {
        when(persistencePort.findById(1L)).thenReturn(Optional.of(entidadGuardada));
        when(persistencePort.save(any(Propuesta.class))).thenAnswer(i -> i.getArgument(0));

        PropuestaResponse response = servicio.enviar(1L);

        assertEquals(EstadoPropuesta.ENVIADA, response.estado());
    }

    @Test
    void aprobar_desdeEnviada_cambiaEstado() {
        entidadGuardada.setEstado(EstadoPropuesta.ENVIADA);
        when(persistencePort.findById(1L)).thenReturn(Optional.of(entidadGuardada));
        when(persistencePort.save(any(Propuesta.class))).thenAnswer(i -> i.getArgument(0));

        PropuestaResponse response = servicio.aprobar(1L);

        assertEquals(EstadoPropuesta.APROBADA, response.estado());
    }

    @Test
    void rechazar_desdeBorrador_lanzaException() {
        when(persistencePort.findById(1L)).thenReturn(Optional.of(entidadGuardada)); // Estado BORRADOR

        assertThrows(IllegalStateException.class, () -> servicio.rechazar(1L));
    }

    @Test
    void cancelar_desdeBorrador_cambiaEstado() {
        when(persistencePort.findById(1L)).thenReturn(Optional.of(entidadGuardada));
        when(persistencePort.save(any(Propuesta.class))).thenAnswer(i -> i.getArgument(0));

        PropuestaResponse response = servicio.cancelar(1L);

        assertEquals(EstadoPropuesta.CANCELADA, response.estado());
    }

    @Test
    void cancelar_desdeAprobada_lanzaException() {
        entidadGuardada.setEstado(EstadoPropuesta.APROBADA);
        when(persistencePort.findById(1L)).thenReturn(Optional.of(entidadGuardada));

        assertThrows(IllegalStateException.class, () -> servicio.cancelar(1L));
    }

    @Test
    void crear_conProductoValido_prefillDesdeCatalogo() {
        DetallePropuestaRequest detalleReq = new DetallePropuestaRequest(null, 3, null, 5L);
        PropuestaRequest request = new PropuestaRequest(
                1L, 1L, "Cliente XYZ", "Juan", LocalDate.now(), LocalDate.now().plusDays(15),
                new BigDecimal("10.00"), "Obs", List.of(detalleReq)
        );
        when(validationPort.existeOportunidad(1L)).thenReturn(true);
        when(validationPort.existeCliente(1L)).thenReturn(true);
        when(productoUseCase.obtener(5L)).thenReturn(productoCatalogo);
        when(persistencePort.countByNumeroPropuestaStartsWith(anyString())).thenReturn(0L);
        when(persistencePort.save(any(Propuesta.class))).thenAnswer(i -> i.getArgument(0));

        PropuestaResponse response = servicio.crear(request);

        ArgumentCaptor<Propuesta> captor = ArgumentCaptor.forClass(Propuesta.class);
        verify(persistencePort).save(captor.capture());
        DetallePropuesta guardado = captor.getValue().getDetalles().get(0);
        assertEquals(5L, guardado.getIdProducto());
        assertEquals("Producto Catálogo", guardado.getDescripcion());
        assertEquals(new BigDecimal("250.00"), guardado.getPrecioUnitario());
        assertEquals(new BigDecimal("750.00"), guardado.getSubtotal());
        assertEquals(new BigDecimal("750.00"), captor.getValue().getSubtotal());
        assertEquals(new BigDecimal("740.00"), captor.getValue().getTotal());
        assertEquals(5L, response.detalles().get(0).productoId());
    }

    @Test
    void crear_conProductoYOverride_respetaValoresEnviados() {
        DetallePropuestaRequest detalleReq = new DetallePropuestaRequest(
                "Servicio personalizado", 2, new BigDecimal("999.00"), 5L);
        PropuestaRequest request = new PropuestaRequest(
                1L, 1L, "Cliente XYZ", "Juan", LocalDate.now(), LocalDate.now().plusDays(15),
                new BigDecimal("10.00"), "Obs", List.of(detalleReq)
        );
        when(validationPort.existeOportunidad(1L)).thenReturn(true);
        when(validationPort.existeCliente(1L)).thenReturn(true);
        when(productoUseCase.obtener(5L)).thenReturn(productoCatalogo);
        when(persistencePort.countByNumeroPropuestaStartsWith(anyString())).thenReturn(0L);
        when(persistencePort.save(any(Propuesta.class))).thenAnswer(i -> i.getArgument(0));

        servicio.crear(request);

        ArgumentCaptor<Propuesta> captor = ArgumentCaptor.forClass(Propuesta.class);
        verify(persistencePort).save(captor.capture());
        DetallePropuesta guardado = captor.getValue().getDetalles().get(0);
        assertEquals(5L, guardado.getIdProducto());
        assertEquals("Servicio personalizado", guardado.getDescripcion());
        assertEquals(new BigDecimal("999.00"), guardado.getPrecioUnitario());
        assertEquals(new BigDecimal("1998.00"), guardado.getSubtotal());
    }

    @Test
    void crear_conProductoInexistente_lanzaNotFound() {
        DetallePropuestaRequest detalleReq = new DetallePropuestaRequest(null, 1, null, 99L);
        PropuestaRequest request = new PropuestaRequest(
                1L, 1L, "Cliente XYZ", "Juan", LocalDate.now(), LocalDate.now().plusDays(15),
                new BigDecimal("10.00"), "Obs", List.of(detalleReq)
        );
        when(validationPort.existeOportunidad(1L)).thenReturn(true);
        when(validationPort.existeCliente(1L)).thenReturn(true);
        when(productoUseCase.obtener(99L))
                .thenThrow(new ResourceNotFoundException("Producto no encontrado: 99"));

        assertThrows(ResourceNotFoundException.class, () -> servicio.crear(request));
        verify(persistencePort, never()).save(any());
    }

    @Test
    void crear_detalleLibreSinProducto_noConsultaCatalogo() {
        when(validationPort.existeOportunidad(1L)).thenReturn(true);
        when(validationPort.existeCliente(1L)).thenReturn(true);
        when(persistencePort.countByNumeroPropuestaStartsWith(anyString())).thenReturn(0L);
        when(persistencePort.save(any(Propuesta.class))).thenReturn(entidadGuardada);

        PropuestaResponse response = servicio.crear(validRequest);

        assertNotNull(response);
        assertNull(response.detalles().get(0).productoId());
        verifyNoInteractions(productoUseCase);
    }

    @Test
    void crear_detalleLibreSinDescripcion_lanzaIllegalArgument() {
        DetallePropuestaRequest detalleReq = new DetallePropuestaRequest(null, 1, new BigDecimal("10.00"), null);
        PropuestaRequest request = new PropuestaRequest(
                1L, 1L, "Cliente XYZ", "Juan", LocalDate.now(), LocalDate.now().plusDays(15),
                new BigDecimal("10.00"), "Obs", List.of(detalleReq)
        );
        when(validationPort.existeOportunidad(1L)).thenReturn(true);
        when(validationPort.existeCliente(1L)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> servicio.crear(request));
        verify(persistencePort, never()).save(any());
    }

    @Test
    void actualizar_conProductoNuevo_aplicaPrefillYRecalculaTotales() {
        DetallePropuestaRequest detalleReq = new DetallePropuestaRequest(null, 4, null, 5L);
        PropuestaRequest request = new PropuestaRequest(
                1L, 1L, "Cliente XYZ", "Juan", LocalDate.now(), LocalDate.now().plusDays(15),
                new BigDecimal("10.00"), "Obs", List.of(detalleReq)
        );
        when(persistencePort.findById(1L)).thenReturn(Optional.of(entidadGuardada));
        when(validationPort.existeOportunidad(1L)).thenReturn(true);
        when(validationPort.existeCliente(1L)).thenReturn(true);
        when(productoUseCase.obtener(5L)).thenReturn(productoCatalogo);
        when(persistencePort.save(any(Propuesta.class))).thenAnswer(i -> i.getArgument(0));

        servicio.actualizar(1L, request);

        ArgumentCaptor<Propuesta> captor = ArgumentCaptor.forClass(Propuesta.class);
        verify(persistencePort).save(captor.capture());
        DetallePropuesta guardado = captor.getValue().getDetalles().get(0);
        assertEquals(5L, guardado.getIdProducto());
        assertEquals(new BigDecimal("250.00"), guardado.getPrecioUnitario());
        assertEquals(new BigDecimal("1000.00"), guardado.getSubtotal());
        assertEquals(new BigDecimal("1000.00"), captor.getValue().getSubtotal());
        assertEquals(new BigDecimal("990.00"), captor.getValue().getTotal());
    }
}
