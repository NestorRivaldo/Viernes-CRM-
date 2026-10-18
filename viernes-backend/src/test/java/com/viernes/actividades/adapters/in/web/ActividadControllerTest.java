package com.viernes.actividades.adapters.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.viernes.actividades.application.dto.ActividadRequest;
import com.viernes.actividades.application.dto.ActividadResponse;
import com.viernes.actividades.application.port.in.ManageActividadUseCase;
import com.viernes.actividades.domain.EstadoActividad;
import com.viernes.actividades.domain.TipoActividad;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ActividadControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private ManageActividadUseCase useCase;

    @InjectMocks
    private ActividadController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new com.viernes.shared.exception.GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void crearActividadValida() throws Exception {
        ActividadRequest request = new ActividadRequest(
                "usr1", 1L, null, null, TipoActividad.REUNION, "Reunión", "Desc", LocalDateTime.now().plusDays(1), null
        );
        ActividadResponse response = new ActividadResponse(
                1L, "usr1", 1L, null, null, TipoActividad.REUNION, "Reunión", "Desc", request.fecha(), EstadoActividad.PENDIENTE, LocalDateTime.now(), false
        );

        when(useCase.crear(any(ActividadRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/actividades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void crearActividadInvalida() throws Exception {
        ActividadRequest request = new ActividadRequest(
                "usr1", 1L, null, null, null, "", "Desc", null, null
        );

        mockMvc.perform(post("/api/v1/actividades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void obtenerActividadExistente() throws Exception {
        ActividadResponse response = new ActividadResponse(
                1L, "usr1", 1L, null, null, TipoActividad.REUNION, "Reunión", "Desc", LocalDateTime.now(), EstadoActividad.PENDIENTE, LocalDateTime.now(), false
        );

        when(useCase.obtener(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/actividades/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void obtenerActividadInexistente() throws Exception {
        when(useCase.obtener(99L)).thenThrow(new ResourceNotFoundException("No encontrado"));

        mockMvc.perform(get("/api/v1/actividades/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listarActividades() throws Exception {
        when(useCase.listarTodos()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/actividades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void actualizarActividad() throws Exception {
        ActividadRequest request = new ActividadRequest(
                "usr1", 1L, null, null, TipoActividad.REUNION, "Actualizado", "Desc", LocalDateTime.now().plusDays(1), null
        );
        ActividadResponse response = new ActividadResponse(
                1L, "usr1", 1L, null, null, TipoActividad.REUNION, "Actualizado", "Desc", request.fecha(), EstadoActividad.PENDIENTE, LocalDateTime.now(), false
        );

        when(useCase.actualizar(eq(1L), any(ActividadRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/actividades/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Actualizado"));
    }

    @Test
    void listarVencidas() throws Exception {
        when(useCase.listarVencidas()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/actividades?vencidas=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
