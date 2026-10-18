package com.viernes.dashboard.application.service;

import com.viernes.dashboard.application.dto.DashboardResumenResponse;
import com.viernes.dashboard.application.port.out.DashboardPersistencePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private DashboardPersistencePort port;

    @InjectMocks
    private DashboardService service;

    @BeforeEach
    void setUp() {
    }

    @Test
    void testResumenVacio() {
        when(port.countClientes()).thenReturn(0L);
        when(port.countLeads()).thenReturn(0L);
        when(port.countLeadsPorEstado()).thenReturn(Collections.emptyList());
        when(port.countOportunidades()).thenReturn(0L);
        when(port.countOportunidadesAbiertas()).thenReturn(0L);
        when(port.countOportunidadesGanadas()).thenReturn(0L);
        when(port.countOportunidadesPerdidas()).thenReturn(0L);
        when(port.sumValorPipeline()).thenReturn(BigDecimal.ZERO);
        when(port.sumValorGanado()).thenReturn(BigDecimal.ZERO);
        when(port.countOportunidadesPorEtapa()).thenReturn(Collections.emptyList());
        when(port.countActividadesPendientes()).thenReturn(0L);
        when(port.countActividadesEnProceso()).thenReturn(0L);
        when(port.countActividadesCompletadas()).thenReturn(0L);
        when(port.countActividadesCanceladas()).thenReturn(0L);
        when(port.countActividadesVencidas()).thenReturn(0L);
        when(port.countActividadesPorTipo()).thenReturn(Collections.emptyList());
        when(port.countActividadesPorEstado()).thenReturn(Collections.emptyList());
        when(port.countPropuestas()).thenReturn(0L);
        when(port.countPropuestasPorEstado()).thenReturn(Collections.emptyList());
        when(port.sumValorPropuestasAprobadas()).thenReturn(BigDecimal.ZERO);

        DashboardResumenResponse resumen = service.getResumen();

        assertNotNull(resumen);
        assertEquals(0L, resumen.getClientes().getTotal());
        assertEquals(0L, resumen.getLeads().getTotal());
        assertEquals(0L, resumen.getOportunidades().getTotal());
        assertEquals(BigDecimal.ZERO, resumen.getOportunidades().getValorPipeline());
        assertNotNull(resumen.getPropuestas());
        assertEquals(0L, resumen.getPropuestas().getTotal());
        assertEquals(BigDecimal.ZERO, resumen.getPropuestas().getValorTotalAprobadas());
    }

    @Test
    void testResumenConDatos() {
        when(port.countClientes()).thenReturn(10L);
        when(port.countOportunidades()).thenReturn(5L);
        when(port.countOportunidadesGanadas()).thenReturn(2L);
        when(port.sumValorGanado()).thenReturn(new BigDecimal("15000.00"));

        DashboardResumenResponse.ClienteResumen kpis = service.getKpis();
        DashboardResumenResponse.OportunidadResumen pipeline = service.getPipeline();

        assertEquals(10L, kpis.getTotal());
        assertEquals(5L, pipeline.getTotal());
        assertEquals(2L, pipeline.getGanadas());
        assertEquals(new BigDecimal("15000.00"), pipeline.getValorGanado());
    }

    @Test
    void testPropuestas() {
        when(port.countPropuestas()).thenReturn(7L);
        when(port.countPropuestasPorEstado()).thenReturn(Collections.emptyList());
        when(port.sumValorPropuestasAprobadas()).thenReturn(new BigDecimal("42000.00"));

        DashboardResumenResponse.PropuestaResumen propuestas = service.getPropuestas();

        assertEquals(7L, propuestas.getTotal());
        assertEquals(new BigDecimal("42000.00"), propuestas.getValorTotalAprobadas());
    }
}
