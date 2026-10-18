package com.viernes.dashboard.application.port.in;

import com.viernes.dashboard.application.dto.DashboardResumenResponse;

public interface DashboardUseCase {
    DashboardResumenResponse getResumen();
    DashboardResumenResponse.ClienteResumen getKpis();
    DashboardResumenResponse.OportunidadResumen getPipeline();
    DashboardResumenResponse.LeadResumen getLeads();
    DashboardResumenResponse.ActividadResumen getActividades();
    DashboardResumenResponse.PropuestaResumen getPropuestas();
}
