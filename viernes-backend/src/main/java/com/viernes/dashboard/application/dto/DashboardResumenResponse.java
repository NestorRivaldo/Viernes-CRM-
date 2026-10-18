package com.viernes.dashboard.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResumenResponse {
    
    private ClienteResumen clientes;
    private LeadResumen leads;
    private OportunidadResumen oportunidades;
    private ActividadResumen actividades;
    private PropuestaResumen propuestas;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClienteResumen {
        private long total;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LeadResumen {
        private long total;
        private List<EstadoCantidadResponse> porEstado;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OportunidadResumen {
        private long total;
        private long abiertas;
        private long ganadas;
        private long perdidas;
        private BigDecimal valorPipeline;
        private BigDecimal valorGanado;
        private List<EtapaCantidadValorResponse> porEtapa;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActividadResumen {
        private long pendientes;
        private long enProceso;
        private long completadas;
        private long canceladas;
        private long vencidas;
        private List<TipoCantidadResponse> porTipo;
        private List<EstadoCantidadResponse> porEstado;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PropuestaResumen {
        private long total;
        private List<EstadoCantidadResponse> porEstado;
        private BigDecimal valorTotalAprobadas;
    }
}
