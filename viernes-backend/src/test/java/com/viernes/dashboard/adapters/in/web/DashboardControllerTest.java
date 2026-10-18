package com.viernes.dashboard.adapters.in.web;

import com.viernes.dashboard.application.dto.DashboardResumenResponse;
import com.viernes.dashboard.application.port.in.DashboardUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    private MockMvc mockMvc;

    @Mock
    private DashboardUseCase useCase;

    @InjectMocks
    private DashboardController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void testResumen() throws Exception {
        DashboardResumenResponse response = new DashboardResumenResponse();
        response.setClientes(new DashboardResumenResponse.ClienteResumen(5));
        
        when(useCase.getResumen()).thenReturn(response);

        mockMvc.perform(get("/api/v1/dashboard/resumen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientes.total").value(5));
    }

    @Test
    void testKpis() throws Exception {
        when(useCase.getKpis()).thenReturn(new DashboardResumenResponse.ClienteResumen(10));

        mockMvc.perform(get("/api/v1/dashboard/kpis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(10));
    }

    @Test
    void testPipeline() throws Exception {
        DashboardResumenResponse.OportunidadResumen response = new DashboardResumenResponse.OportunidadResumen();
        response.setTotal(15);
        when(useCase.getPipeline()).thenReturn(response);

        mockMvc.perform(get("/api/v1/dashboard/pipeline"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(15));
    }

    @Test
    void testLeads() throws Exception {
        DashboardResumenResponse.LeadResumen response = new DashboardResumenResponse.LeadResumen();
        response.setTotal(20);
        when(useCase.getLeads()).thenReturn(response);

        mockMvc.perform(get("/api/v1/dashboard/leads"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(20));
    }

    @Test
    void testActividades() throws Exception {
        DashboardResumenResponse.ActividadResumen response = new DashboardResumenResponse.ActividadResumen();
        response.setPendientes(30);
        when(useCase.getActividades()).thenReturn(response);

        mockMvc.perform(get("/api/v1/dashboard/actividades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendientes").value(30));
    }
}
