package com.viernes.leads.adapters.in.web;

import com.viernes.leads.application.dto.LeadRequest;
import com.viernes.leads.application.dto.LeadResponse;
import com.viernes.leads.application.port.in.ManageLeadUseCase;
import com.viernes.leads.domain.EstadoLead;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class LeadControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ManageLeadUseCase useCase;

    @InjectMocks
    private LeadController controller;

    private LeadResponse validResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new com.viernes.shared.exception.GlobalExceptionHandler())
                .build();

        validResponse = new LeadResponse(
                1L, "Ana", "Empresa SA", "ana@mail.com", "999888777", "WEB",
                EstadoLead.NUEVO, "usr1", LocalDate.now(), LocalDateTime.now(), null, null, null
        );
    }

    @Test
    void listarLeads() throws Exception {
        when(useCase.listar()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/leads"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void listarLeads_conParametroQ_llamaBuscar() throws Exception {
        when(useCase.buscar("ana")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/leads?q=ana"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(useCase).buscar("ana");
    }

    @Test
    void listarLeads_conParametroEstado_llamaFiltrarPorEstado() throws Exception {
        when(useCase.filtrarPorEstado(EstadoLead.NUEVO)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/leads?estado=NUEVO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(useCase).filtrarPorEstado(EstadoLead.NUEVO);
    }

    @Test
    void obtenerLeadExistente() throws Exception {
        when(useCase.obtener(1L)).thenReturn(validResponse);

        mockMvc.perform(get("/api/v1/leads/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.nombre").value("Ana"));
    }

    @Test
    void obtenerLeadInexistente() throws Exception {
        when(useCase.obtener(99L)).thenThrow(new ResourceNotFoundException("No encontrado"));

        mockMvc.perform(get("/api/v1/leads/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void crearLeadValido() throws Exception {
        when(useCase.crear(any(LeadRequest.class))).thenReturn(validResponse);

        mockMvc.perform(post("/api/v1/leads")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Ana\",\"empresa\":\"Empresa SA\",\"correo\":\"ana@mail.com\",\"telefono\":\"999888777\",\"fuente\":\"WEB\",\"responsable\":\"usr1\",\"fecha\":\"2026-10-01\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.estado").value("NUEVO"));
    }

    @Test
    void crearLeadInvalido_sinNombre() throws Exception {
        mockMvc.perform(post("/api/v1/leads")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"\",\"correo\":\"ana@mail.com\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void actualizarLead() throws Exception {
        when(useCase.actualizar(eq(1L), any(LeadRequest.class))).thenReturn(validResponse);

        mockMvc.perform(put("/api/v1/leads/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Ana Maria\",\"empresa\":\"Empresa SA\",\"correo\":\"ana@mail.com\",\"telefono\":\"999888777\",\"fuente\":\"WEB\",\"responsable\":\"usr1\",\"fecha\":\"2026-10-01\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void cambiarEstadoLead() throws Exception {
        LeadResponse contactadoResponse = new LeadResponse(
                1L, "Ana", "Empresa SA", "ana@mail.com", "999888777", "WEB",
                EstadoLead.CONTACTADO, "usr1", LocalDate.now(), LocalDateTime.now(), null, null, null
        );
        when(useCase.cambiarEstado(eq(1L), eq(EstadoLead.CONTACTADO))).thenReturn(contactadoResponse);

        mockMvc.perform(patch("/api/v1/leads/1/estado?estado=CONTACTADO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CONTACTADO"));
    }

    @Test
    void eliminarLead() throws Exception {
        mockMvc.perform(delete("/api/v1/leads/1"))
                .andExpect(status().isNoContent());

        verify(useCase).eliminar(1L);
    }

    @Test
    void obtenerLeadConFuente() throws Exception {
        LeadResponse conFuente = new LeadResponse(
                1L, "Ana", "Empresa SA", "ana@mail.com", "999888777", "WEB",
                EstadoLead.NUEVO, "usr1", LocalDate.now(), LocalDateTime.now(),
                2L, "WEB", "Sitio Web"
        );
        when(useCase.obtener(1L)).thenReturn(conFuente);

        mockMvc.perform(get("/api/v1/leads/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fuenteId").value(2L))
                .andExpect(jsonPath("$.fuenteCodigo").value("WEB"))
                .andExpect(jsonPath("$.fuenteNombre").value("Sitio Web"));
    }
}
