package com.viernes.productos.application.service;

import com.viernes.productos.application.dto.ProductoRequest;
import com.viernes.productos.application.dto.ProductoResponse;
import com.viernes.productos.application.port.out.ProductoPersistencePort;
import com.viernes.productos.domain.EstadoProducto;
import com.viernes.productos.domain.Producto;
import com.viernes.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock
    private ProductoPersistencePort persistencePort;

    @InjectMocks
    private ProductoService servicio;

    private ProductoRequest validRequest;
    private Producto entidadGuardada;

    @BeforeEach
    void setUp() {
        validRequest = new ProductoRequest(
                "PROD-001", "Laptop Pro", "Laptop de alto rendimiento", "EQUIPOS",
                new BigDecimal("3500.00"), null
        );

        entidadGuardada = Producto.builder()
                .id(1L)
                .codigo("PROD-001")
                .nombre("Laptop Pro")
                .descripcion("Laptop de alto rendimiento")
                .categoria("EQUIPOS")
                .precioBase(new BigDecimal("3500.00"))
                .estado(EstadoProducto.ACTIVO)
                .creadoEn(LocalDateTime.now())
                .build();
    }

    @Test
    void listar_retornaTodosLosProductos() {
        when(persistencePort.findAll()).thenReturn(List.of(entidadGuardada));

        List<ProductoResponse> resultado = servicio.listar();

        assertEquals(1, resultado.size());
        assertEquals("PROD-001", resultado.get(0).codigo());
    }

    @Test
    void obtener_existente_retornaProducto() {
        when(persistencePort.findById(1L)).thenReturn(Optional.of(entidadGuardada));

        ProductoResponse response = servicio.obtener(1L);

        assertNotNull(response);
        assertEquals("Laptop Pro", response.nombre());
    }

    @Test
    void obtener_inexistente_lanzaNotFound() {
        when(persistencePort.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> servicio.obtener(99L));
    }

    @Test
    void crear_sinEstado_asignaActivoPorDefecto() {
        when(persistencePort.existsByCodigo("PROD-001")).thenReturn(false);
        when(persistencePort.save(any(Producto.class))).thenAnswer(i -> i.getArgument(0));

        ProductoResponse response = servicio.crear(validRequest);

        assertNotNull(response);
        assertEquals(EstadoProducto.ACTIVO, response.estado());
        assertEquals("Laptop Pro", response.nombre());
    }

    @Test
    void crear_codigoDuplicado_lanzaIllegalArgument() {
        when(persistencePort.existsByCodigo("PROD-001")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> servicio.crear(validRequest));
        verify(persistencePort, never()).save(any());
    }

    @Test
    void actualizar_existente_retornaActualizado() {
        ProductoRequest cambios = new ProductoRequest(
                "PROD-001", "Laptop Pro X", "Laptop renovada", "EQUIPOS",
                new BigDecimal("4200.00"), EstadoProducto.INACTIVO
        );
        when(persistencePort.findById(1L)).thenReturn(Optional.of(entidadGuardada));
        when(persistencePort.save(any(Producto.class))).thenAnswer(i -> i.getArgument(0));

        ProductoResponse response = servicio.actualizar(1L, cambios);

        assertNotNull(response);
        assertEquals("Laptop Pro X", response.nombre());
        assertEquals(EstadoProducto.INACTIVO, response.estado());
        verify(persistencePort).save(any(Producto.class));
    }

    @Test
    void actualizar_codigoDuplicado_lanzaIllegalArgument() {
        ProductoRequest cambios = new ProductoRequest(
                "PROD-999", "Laptop Pro X", "Laptop renovada", "EQUIPOS",
                new BigDecimal("4200.00"), EstadoProducto.INACTIVO
        );
        when(persistencePort.findById(1L)).thenReturn(Optional.of(entidadGuardada));
        when(persistencePort.existsByCodigo("PROD-999")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> servicio.actualizar(1L, cambios));
        verify(persistencePort, never()).save(any());
    }

    @Test
    void eliminar_existente_eliminaCorrectamente() {
        when(persistencePort.findById(1L)).thenReturn(Optional.of(entidadGuardada));

        servicio.eliminar(1L);

        verify(persistencePort).deleteById(1L);
    }

    @Test
    void eliminar_inexistente_lanzaNotFound() {
        when(persistencePort.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> servicio.eliminar(99L));
        verify(persistencePort, never()).deleteById(any());
    }
}
