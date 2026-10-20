package com.viernes.marketing.adapters.in.web;

import com.viernes.leads.domain.EstadoLead;
import com.viernes.marketing.application.dto.CampaniaLeadRequest;
import com.viernes.marketing.application.dto.CampaniaLeadResponse;
import com.viernes.marketing.application.dto.CampaniaRequest;
import com.viernes.marketing.application.dto.CampaniaResponse;
import com.viernes.marketing.application.port.in.ManageCampaniaUseCase;
import com.viernes.marketing.domain.EstadoCampania;
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
class CampaniaControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ManageCampaniaUseCase useCase;

    @InjectMocks
    private CampaniaController controller;

    private CampaniaResponse validResponse;
    private CampaniaLeadResponse leadAsociadoResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new com.viernes.shared.exception.GlobalExceptionHandler())
                .build();

        validResponse = new CampaniaResponse(
                1L, "Lanzamiento Q4", "Campania de lanzamiento",
                LocalDate.now(), LocalDate.now().plusDays(30),
                EstadoCampania.BORRADOR, LocalDateTime.now()
        );

        leadAsociadoResponse = new CampaniaLeadResponse(
                5L, 1L, 7L, "Ana", "Empresa SA", "ana@mail.com",
                EstadoLead.NUEVO, LocalDateTime.now()
        );
    }

    @Test
    void listarCampanias() throws Exception {
        when(useCase.listar()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/campanias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void obtenerCampaniaExistente() throws Exception {
        when(useCase.obtener(1L)).thenReturn(validResponse);

        mockMvc.perform(get("/api/v1/campanias/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.nombre").value("Lanzamiento Q4"))
                .andExpect(jsonPath("$.estado").value("BORRADOR"));
    }

    @Test
    void obtenerCampaniaInexistente() throws Exception {
        when(useCase.obtener(99L)).thenThrow(new ResourceNotFoundException("No encontrada"));

        mockMvc.perform(get("/api/v1/campanias/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void crearCampaniaValida() throws Exception {
        when(useCase.crear(any(CampaniaRequest.class))).thenReturn(validResponse);

        mockMvc.perform(post("/api/v1/campanias")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Lanzamiento Q4\",\"descripcion\":\"Campania de lanzamiento\",\"fechaInicio\":\"2026-11-01\",\"fechaFin\":\"2026-11-30\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.estado").value("BORRADOR"));
    }

    @Test
    void crearCampaniaInvalida_sinNombre() throws Exception {
        mockMvc.perform(post("/api/v1/campanias")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"\",\"descripcion\":\"Desc\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void actualizarCampania() throws Exception {
        when(useCase.actualizar(eq(1L), any(CampaniaRequest.class))).thenReturn(validResponse);

        mockMvc.perform(put("/api/v1/campanias/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Lanzamiento Q4 Plus\",\"descripcion\":\"Desc\",\"fechaInicio\":\"2026-11-01\",\"fechaFin\":\"2026-12-31\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void cambiarEstadoCampania() throws Exception {
        CampaniaResponse activaResponse = new CampaniaResponse(
                1L, "Lanzamiento Q4", "Campania de lanzamiento",
                LocalDate.now(), LocalDate.now().plusDays(30),
                EstadoCampania.ACTIVA, LocalDateTime.now()
        );
        when(useCase.cambiarEstado(eq(1L), eq(EstadoCampania.ACTIVA))).thenReturn(activaResponse);

        mockMvc.perform(patch("/api/v1/campanias/1/estado?estado=ACTIVA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ACTIVA"));
    }

    @Test
    void eliminarCampania() throws Exception {
        mockMvc.perform(delete("/api/v1/campanias/1"))
                .andExpect(status().isNoContent());

        verify(useCase).eliminar(1L);
    }

    @Test
    void listarLeadsDeCampania() throws Exception {
        when(useCase.listarLeads(1L)).thenReturn(Collections.singletonList(leadAsociadoResponse));

        mockMvc.perform(get("/api/v1/campanias/1/leads"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].leadId").value(7L))
                .andExpect(jsonPath("$[0].leadNombre").value("Ana"));
    }

    @Test
    void asociarLeadValido() throws Exception {
        when(useCase.asociarLead(eq(1L), eq(7L))).thenReturn(leadAsociadoResponse);

        mockMvc.perform(post("/api/v1/campanias/1/leads")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"leadId\":7}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.campaniaId").value(1L))
                .andExpect(jsonPath("$.leadId").value(7L));
    }

    @Test
    void asociarLeadInvalido_sinLeadId() throws Exception {
        mockMvc.perform(post("/api/v1/campanias/1/leads")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void asociarLeadDuplicado() throws Exception {
        when(useCase.asociarLead(eq(1L), eq(7L)))
                .thenThrow(new IllegalArgumentException("El lead 7 ya está asociado a la campaña: 1"));

        mockMvc.perform(post("/api/v1/campanias/1/leads")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"leadId\":7}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void desasociarLead() throws Exception {
        mockMvc.perform(delete("/api/v1/campanias/1/leads/7"))
                .andExpect(status().isNoContent());

        verify(useCase).desasociarLead(1L, 7L);
    }
}
