package com.viernes.dashboard.application.port.out;

import com.viernes.dashboard.application.dto.EstadoCantidadResponse;
import com.viernes.dashboard.application.dto.EtapaCantidadValorResponse;
import com.viernes.dashboard.application.dto.TipoCantidadResponse;

import java.math.BigDecimal;
import java.util.List;

public interface DashboardPersistencePort {
    long countClientes();
    
    long countLeads();
    List<EstadoCantidadResponse> countLeadsPorEstado();
    
    long countOportunidades();
    long countOportunidadesAbiertas();
    long countOportunidadesGanadas();
    long countOportunidadesPerdidas();
    BigDecimal sumValorPipeline();
    BigDecimal sumValorGanado();
    List<EtapaCantidadValorResponse> countOportunidadesPorEtapa();
    
    long countActividadesPendientes();
    long countActividadesEnProceso();
    long countActividadesCompletadas();
    long countActividadesCanceladas();
    long countActividadesVencidas();
    List<EstadoCantidadResponse> countActividadesPorEstado();
    List<TipoCantidadResponse> countActividadesPorTipo();

    long countPropuestas();
    List<EstadoCantidadResponse> countPropuestasPorEstado();
    BigDecimal sumValorPropuestasAprobadas();
}
