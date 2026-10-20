package com.viernes.productos.adapters.in.web;

import com.viernes.productos.application.dto.ProductoRequest;
import com.viernes.productos.application.dto.ProductoResponse;
import com.viernes.productos.application.port.in.ManageProductoUseCase;
import com.viernes.productos.domain.EstadoProducto;
import com.viernes.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ProductoControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ManageProductoUseCase useCase;

    @InjectMocks
    private ProductoController controller;

    private ProductoResponse validResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new com.viernes.shared.exception.GlobalExceptionHandler())
                .build();

        validResponse = new ProductoResponse(
                1L, "PROD-001", "Laptop Pro", "Laptop de alto rendimiento", "EQUIPOS",
                new BigDecimal("3500.00"), EstadoProducto.ACTIVO, LocalDateTime.now()
        );
    }

    @Test
    void listarProductos() throws Exception {
        when(useCase.listar()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void listarProductos_conParametroQ_llamaBuscar() throws Exception {
        when(useCase.buscar("laptop")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/productos?q=laptop"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(useCase).buscar("laptop");
    }

    @Test
    void listarProductos_conParametroEstado_llamaListarPorEstado() throws Exception {
        when(useCase.listarPorEstado(EstadoProducto.ACTIVO)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/productos?estado=ACTIVO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(useCase).listarPorEstado(EstadoProducto.ACTIVO);
    }

    @Test
    void obtenerProductoExistente() throws Exception {
        when(useCase.obtener(1L)).thenReturn(validResponse);

        mockMvc.perform(get("/api/v1/productos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.nombre").value("Laptop Pro"));
    }

    @Test
    void obtenerProductoInexistente() throws Exception {
        when(useCase.obtener(99L)).thenThrow(new ResourceNotFoundException("No encontrado"));

        mockMvc.perform(get("/api/v1/productos/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void crearProductoValido() throws Exception {
        when(useCase.crear(any(ProductoRequest.class))).thenReturn(validResponse);

        mockMvc.perform(post("/api/v1/productos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigo\":\"PROD-001\",\"nombre\":\"Laptop Pro\",\"descripcion\":\"Laptop de alto rendimiento\",\"categoria\":\"EQUIPOS\",\"precioBase\":3500.00}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.estado").value("ACTIVO"));
    }

    @Test
    void crearProductoInvalido_sinNombre() throws Exception {
        mockMvc.perform(post("/api/v1/productos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigo\":\"PROD-001\",\"nombre\":\"\",\"precioBase\":3500.00}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crearProductoInvalido_precioNegativo() throws Exception {
        mockMvc.perform(post("/api/v1/productos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigo\":\"PROD-001\",\"nombre\":\"Laptop Pro\",\"precioBase\":-10}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crearProducto_codigoDuplicado_retornaBadRequest() throws Exception {
        when(useCase.crear(any(ProductoRequest.class)))
                .thenThrow(new IllegalArgumentException("Ya existe un producto con el código: PROD-001"));

        mockMvc.perform(post("/api/v1/productos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigo\":\"PROD-001\",\"nombre\":\"Laptop Pro\",\"precioBase\":3500.00}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void actualizarProducto() throws Exception {
        when(useCase.actualizar(eq(1L), any(ProductoRequest.class))).thenReturn(validResponse);

        mockMvc.perform(put("/api/v1/productos/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigo\":\"PROD-001\",\"nombre\":\"Laptop Pro X\",\"descripcion\":\"Laptop renovada\",\"categoria\":\"EQUIPOS\",\"precioBase\":4200.00,\"estado\":\"INACTIVO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void eliminarProducto() throws Exception {
        mockMvc.perform(delete("/api/v1/productos/1"))
                .andExpect(status().isNoContent());

        verify(useCase).eliminar(1L);
    }
}
