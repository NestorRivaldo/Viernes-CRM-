package com.viernes.dashboard.application.service;

import com.viernes.dashboard.application.dto.DashboardResumenResponse;
import com.viernes.dashboard.application.port.in.DashboardUseCase;
import com.viernes.dashboard.application.port.out.DashboardPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardService implements DashboardUseCase {

    private final DashboardPersistencePort port;

    @Override
    public DashboardResumenResponse getResumen() {
        return DashboardResumenResponse.builder()
                .clientes(getKpis())
                .leads(getLeads())
                .oportunidades(getPipeline())
                .actividades(getActividades())
                .propuestas(getPropuestas())
                .build();
    }

    @Override
    public DashboardResumenResponse.ClienteResumen getKpis() {
        return DashboardResumenResponse.ClienteResumen.builder()
                .total(port.countClientes())
                .build();
    }

    @Override
    public DashboardResumenResponse.OportunidadResumen getPipeline() {
        return DashboardResumenResponse.OportunidadResumen.builder()
                .total(port.countOportunidades())
                .abiertas(port.countOportunidadesAbiertas())
                .ganadas(port.countOportunidadesGanadas())
                .perdidas(port.countOportunidadesPerdidas())
                .valorPipeline(port.sumValorPipeline())
                .valorGanado(port.sumValorGanado())
                .porEtapa(port.countOportunidadesPorEtapa())
                .build();
    }

    @Override
    public DashboardResumenResponse.LeadResumen getLeads() {
        return DashboardResumenResponse.LeadResumen.builder()
                .total(port.countLeads())
                .porEstado(port.countLeadsPorEstado())
                .build();
    }

    @Override
    public DashboardResumenResponse.ActividadResumen getActividades() {
        return DashboardResumenResponse.ActividadResumen.builder()
                .pendientes(port.countActividadesPendientes())
                .enProceso(port.countActividadesEnProceso())
                .completadas(port.countActividadesCompletadas())
                .canceladas(port.countActividadesCanceladas())
                .vencidas(port.countActividadesVencidas())
                .porTipo(port.countActividadesPorTipo())
                .porEstado(port.countActividadesPorEstado())
                .build();
    }

    @Override
    public DashboardResumenResponse.PropuestaResumen getPropuestas() {
        java.math.BigDecimal valorAprobadas = port.sumValorPropuestasAprobadas();
        return DashboardResumenResponse.PropuestaResumen.builder()
                .total(port.countPropuestas())
                .porEstado(port.countPropuestasPorEstado())
                .valorTotalAprobadas(valorAprobadas != null ? valorAprobadas : java.math.BigDecimal.ZERO)
                .build();
    }
}
