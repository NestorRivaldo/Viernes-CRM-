package com.viernes.leads.adapters.in.web;

import com.viernes.leads.application.dto.FuenteRequest;
import com.viernes.leads.application.dto.FuenteResponse;
import com.viernes.leads.application.port.in.ManageFuenteUseCase;
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

import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class FuenteControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ManageFuenteUseCase useCase;

    @InjectMocks
    private FuenteController controller;

    private FuenteResponse validResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new com.viernes.shared.exception.GlobalExceptionHandler())
                .build();

        validResponse = new FuenteResponse(
                1L, "WEB", "Sitio Web", "Formulario web", true, LocalDateTime.now()
        );
    }

    @Test
    void listarFuentes() throws Exception {
        when(useCase.listar(null)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/fuentes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void listarFuentes_conParametroActiva_llamaListarConFiltro() throws Exception {
        when(useCase.listar(true)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/fuentes?activa=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(useCase).listar(true);
    }

    @Test
    void obtenerFuenteExistente() throws Exception {
        when(useCase.obtener(1L)).thenReturn(validResponse);

        mockMvc.perform(get("/api/v1/fuentes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.codigo").value("WEB"));
    }

    @Test
    void obtenerFuenteInexistente() throws Exception {
        when(useCase.obtener(99L)).thenThrow(new ResourceNotFoundException("No encontrada"));

        mockMvc.perform(get("/api/v1/fuentes/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void crearFuenteValida() throws Exception {
        when(useCase.crear(any(FuenteRequest.class))).thenReturn(validResponse);

        mockMvc.perform(post("/api/v1/fuentes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigo\":\"WEB\",\"nombre\":\"Sitio Web\",\"descripcion\":\"Formulario web\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.activa").value(true));
    }

    @Test
    void crearFuenteInvalida_sinCodigo() throws Exception {
        mockMvc.perform(post("/api/v1/fuentes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigo\":\"\",\"nombre\":\"Sitio Web\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crearFuenteDuplicada() throws Exception {
        when(useCase.crear(any(FuenteRequest.class)))
                .thenThrow(new IllegalArgumentException("Ya existe una fuente con el código: WEB"));

        mockMvc.perform(post("/api/v1/fuentes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigo\":\"WEB\",\"nombre\":\"Sitio Web\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void actualizarFuente() throws Exception {
        when(useCase.actualizar(eq(1L), any(FuenteRequest.class))).thenReturn(validResponse);

        mockMvc.perform(put("/api/v1/fuentes/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigo\":\"WEB\",\"nombre\":\"Sitio Web Corporativo\",\"activa\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void eliminarFuente() throws Exception {
        mockMvc.perform(delete("/api/v1/fuentes/1"))
                .andExpect(status().isNoContent());

        verify(useCase).eliminar(1L);
    }
}
