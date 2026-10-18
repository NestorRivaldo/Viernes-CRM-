package com.viernes.dashboard.adapters.out.persistence;

import com.viernes.dashboard.application.dto.EstadoCantidadResponse;
import com.viernes.dashboard.application.dto.EtapaCantidadValorResponse;
import com.viernes.dashboard.application.dto.TipoCantidadResponse;
import com.viernes.dashboard.application.port.out.DashboardPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DashboardPersistenceAdapter implements DashboardPersistencePort {

    private final DashboardQueryRepository repository;

    @Override
    public long countClientes() {
        return repository.countClientes();
    }

    @Override
    public long countLeads() {
        return repository.countLeads();
    }

    @Override
    public List<EstadoCantidadResponse> countLeadsPorEstado() {
        return repository.countLeadsPorEstado();
    }

    @Override
    public long countOportunidades() {
        return repository.countOportunidades();
    }

    @Override
    public long countOportunidadesAbiertas() {
        return repository.countOportunidadesAbiertas();
    }

    @Override
    public long countOportunidadesGanadas() {
        return repository.countOportunidadesGanadas();
    }

    @Override
    public long countOportunidadesPerdidas() {
        return repository.countOportunidadesPerdidas();
    }

    @Override
    public BigDecimal sumValorPipeline() {
        return repository.sumValorPipeline();
    }

    @Override
    public BigDecimal sumValorGanado() {
        return repository.sumValorGanado();
    }

    @Override
    public List<EtapaCantidadValorResponse> countOportunidadesPorEtapa() {
        return repository.countOportunidadesPorEtapa();
    }

    @Override
    public long countActividadesPendientes() {
        return repository.countActividadesPendientes();
    }

    @Override
    public long countActividadesEnProceso() {
        return repository.countActividadesEnProceso();
    }

    @Override
    public long countActividadesCompletadas() {
        return repository.countActividadesCompletadas();
    }

    @Override
    public long countActividadesCanceladas() {
        return repository.countActividadesCanceladas();
    }

    @Override
    public long countActividadesVencidas() {
        return repository.countActividadesVencidas();
    }

    @Override
    public List<EstadoCantidadResponse> countActividadesPorEstado() {
        return repository.countActividadesPorEstado();
    }

    @Override
    public List<TipoCantidadResponse> countActividadesPorTipo() {
        return repository.countActividadesPorTipo();
    }

    @Override
    public long countPropuestas() {
        return repository.countPropuestas();
    }

    @Override
    public List<EstadoCantidadResponse> countPropuestasPorEstado() {
        return repository.countPropuestasPorEstado();
    }

    @Override
    public BigDecimal sumValorPropuestasAprobadas() {
        return repository.sumValorPropuestasAprobadas();
    }
}
