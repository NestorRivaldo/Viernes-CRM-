package com.viernes.propuestas.adapters.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.viernes.propuestas.application.dto.DetallePropuestaRequest;
import com.viernes.propuestas.application.dto.PropuestaRequest;
import com.viernes.propuestas.application.dto.PropuestaResponse;
import com.viernes.propuestas.application.dto.DetallePropuestaResponse;
import com.viernes.propuestas.application.port.in.ManagePropuestaUseCase;
import com.viernes.propuestas.domain.EstadoPropuesta;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PropuestaControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private ManagePropuestaUseCase useCase;

    @InjectMocks
    private PropuestaController controller;

    private PropuestaRequest validRequest;
    private PropuestaResponse validResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new com.viernes.shared.exception.GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        DetallePropuestaRequest detalleReq = new DetallePropuestaRequest("Servicio", 2, new BigDecimal("100.00"));
        validRequest = new PropuestaRequest(
                1L, 1L, "Cliente XYZ", "Juan", LocalDate.now(), LocalDate.now().plusDays(15),
                new BigDecimal("10.00"), "Obs", List.of(detalleReq)
        );

        DetallePropuestaResponse detalleResp = new DetallePropuestaResponse(1L, "Servicio", 2, new BigDecimal("100.00"), new BigDecimal("200.00"));
        validResponse = new PropuestaResponse(
                1L, "PROP-2026-0001", 1L, 1L, "Cliente XYZ", EstadoPropuesta.BORRADOR,
                1, new BigDecimal("200.00"), new BigDecimal("10.00"), new BigDecimal("190.00"),
                "Juan", LocalDate.now(), LocalDate.now().plusDays(15), "Obs", LocalDateTime.now(), List.of(detalleResp)
        );
    }

    @Test
    void crearPropuestaValida() throws Exception {
        when(useCase.crear(any(PropuestaRequest.class))).thenReturn(validResponse);

        mockMvc.perform(post("/api/v1/propuestas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.numeroPropuesta").value("PROP-2026-0001"));
    }

    @Test
    void crearPropuestaInvalida_sinDetalles() throws Exception {
        PropuestaRequest invalidRequest = new PropuestaRequest(
                1L, 1L, "Cliente XYZ", "Juan", LocalDate.now(), LocalDate.now().plusDays(15),
                new BigDecimal("10.00"), "Obs", Collections.emptyList() // Inválido: @NotEmpty
        );

        mockMvc.perform(post("/api/v1/propuestas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void obtenerPropuestaExistente() throws Exception {
        when(useCase.obtener(1L)).thenReturn(validResponse);

        mockMvc.perform(get("/api/v1/propuestas/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void obtenerPropuestaInexistente() throws Exception {
        when(useCase.obtener(99L)).thenThrow(new ResourceNotFoundException("No encontrado"));

        mockMvc.perform(get("/api/v1/propuestas/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listarPropuestas() throws Exception {
        when(useCase.listar()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/propuestas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void actualizarPropuesta() throws Exception {
        when(useCase.actualizar(eq(1L), any(PropuestaRequest.class))).thenReturn(validResponse);

        mockMvc.perform(put("/api/v1/propuestas/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void enviarPropuesta() throws Exception {
        PropuestaResponse enviadaResponse = new PropuestaResponse(
                1L, "PROP-2026-0001", 1L, 1L, "Cliente XYZ", EstadoPropuesta.ENVIADA, // Estado cambiado
                1, new BigDecimal("200.00"), new BigDecimal("10.00"), new BigDecimal("190.00"),
                "Juan", LocalDate.now(), LocalDate.now().plusDays(15), "Obs", LocalDateTime.now(), Collections.emptyList()
        );
        when(useCase.enviar(1L)).thenReturn(enviadaResponse);

        mockMvc.perform(patch("/api/v1/propuestas/1/enviar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ENVIADA"));
    }
}
