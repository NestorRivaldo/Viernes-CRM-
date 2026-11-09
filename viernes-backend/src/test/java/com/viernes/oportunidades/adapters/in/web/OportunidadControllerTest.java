package com.viernes.oportunidades.adapters.in.web;

import com.viernes.oportunidades.application.dto.OportunidadRequest;
import com.viernes.oportunidades.application.dto.OportunidadResponse;
import com.viernes.oportunidades.application.port.in.ManageOportunidadUseCase;
import com.viernes.oportunidades.domain.EtapaOportunidad;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class OportunidadControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ManageOportunidadUseCase useCase;

    @InjectMocks
    private OportunidadController controller;

    private OportunidadResponse validResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new com.viernes.shared.exception.GlobalExceptionHandler())
                .build();

        validResponse = new OportunidadResponse(
                1L, "Venta ERP", "Cliente XYZ", new BigDecimal("10000.00"), 50,
                new BigDecimal("5000.00"), EtapaOportunidad.PROSPECCION, "Juan",
                LocalDate.now().plusDays(30), null, LocalDateTime.now()
        );
    }

    @Test
    void listarOportunidades() throws Exception {
        when(useCase.listar()).thenReturn(List.of(validResponse));

        mockMvc.perform(get("/api/v1/oportunidades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].nombre").value("Venta ERP"));
    }

    @Test
    void buscarOportunidades_conQ() throws Exception {
        when(useCase.buscar("ERP")).thenReturn(List.of(validResponse));

        mockMvc.perform(get("/api/v1/oportunidades").param("q", "ERP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].nombre").value("Venta ERP"));
    }

    @Test
    void filtrarOportunidades_conEtapa() throws Exception {
        when(useCase.filtrarPorEtapa(EtapaOportunidad.PROSPECCION)).thenReturn(List.of(validResponse));

        mockMvc.perform(get("/api/v1/oportunidades").param("etapa", "PROSPECCION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].etapa").value("PROSPECCION"));
    }

    @Test
    void obtenerOportunidadExistente() throws Exception {
        when(useCase.obtener(1L)).thenReturn(validResponse);

        mockMvc.perform(get("/api/v1/oportunidades/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.valorPonderado").value(5000.00));
    }

    @Test
    void obtenerOportunidadInexistente() throws Exception {
        when(useCase.obtener(99L)).thenThrow(new ResourceNotFoundException("No encontrado"));

        mockMvc.perform(get("/api/v1/oportunidades/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void crearOportunidadValida() throws Exception {
        when(useCase.crear(any(OportunidadRequest.class))).thenReturn(validResponse);

        mockMvc.perform(post("/api/v1/oportunidades")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\": \"Venta ERP\", \"clienteNombre\": \"Cliente XYZ\", \"valor\": 10000.00, \"probabilidad\": 50, \"responsable\": \"Juan\", \"fechaCierre\": \"2026-12-31\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.etapa").value("PROSPECCION"));
    }

    @Test
    void crearOportunidadInvalida_nombreEnBlanco() throws Exception {
        mockMvc.perform(post("/api/v1/oportunidades")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\": \"\", \"clienteNombre\": \"Cliente XYZ\", \"valor\": 10000.00, \"probabilidad\": 50}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void actualizarOportunidad() throws Exception {
        when(useCase.actualizar(eq(1L), any(OportunidadRequest.class))).thenReturn(validResponse);

        mockMvc.perform(put("/api/v1/oportunidades/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\": \"Venta ERP Actualizada\", \"clienteNombre\": \"Cliente XYZ\", \"valor\": 20000.00, \"probabilidad\": 75, \"etapa\": \"NEGOCIACION\", \"responsable\": \"Maria\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void cambiarEtapaOportunidad() throws Exception {
        OportunidadResponse etapaResponse = new OportunidadResponse(
                1L, "Venta ERP", "Cliente XYZ", new BigDecimal("10000.00"), 50,
                new BigDecimal("5000.00"), EtapaOportunidad.NEGOCIACION, "Juan",
                LocalDate.now().plusDays(30), null, LocalDateTime.now()
        );
        when(useCase.cambiarEtapa(eq(1L), eq(EtapaOportunidad.NEGOCIACION))).thenReturn(etapaResponse);

        mockMvc.perform(patch("/api/v1/oportunidades/1/etapa").param("etapa", "NEGOCIACION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapa").value("NEGOCIACION"));
    }

    @Test
    void eliminarOportunidad() throws Exception {
        mockMvc.perform(delete("/api/v1/oportunidades/1"))
                .andExpect(status().isNoContent());

        verify(useCase).eliminar(1L);
    }
}
