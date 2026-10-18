package com.viernes.dashboard.adapters.in.web;

import com.viernes.dashboard.application.dto.DashboardResumenResponse;
import com.viernes.dashboard.application.port.in.DashboardUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Dashboard", description = "KPIs y metricas del CRM")
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardUseCase useCase;

    @Operation(summary = "Obtener resumen completo")
    @GetMapping("/resumen")
    public ResponseEntity<DashboardResumenResponse> resumen() {
        return ResponseEntity.ok(useCase.getResumen());
    }

    @Operation(summary = "Obtener KPIs principales (Clientes)")
    @GetMapping("/kpis")
    public ResponseEntity<DashboardResumenResponse.ClienteResumen> kpis() {
        return ResponseEntity.ok(useCase.getKpis());
    }

    @Operation(summary = "Obtener resumen de pipeline")
    @GetMapping("/pipeline")
    public ResponseEntity<DashboardResumenResponse.OportunidadResumen> pipeline() {
        return ResponseEntity.ok(useCase.getPipeline());
    }

    @Operation(summary = "Obtener resumen de leads")
    @GetMapping("/leads")
    public ResponseEntity<DashboardResumenResponse.LeadResumen> leads() {
        return ResponseEntity.ok(useCase.getLeads());
    }

    @Operation(summary = "Obtener resumen de actividades")
    @GetMapping("/actividades")
    public ResponseEntity<DashboardResumenResponse.ActividadResumen> actividades() {
        return ResponseEntity.ok(useCase.getActividades());
    }

    @Operation(summary = "Obtener resumen de propuestas")
    @GetMapping("/propuestas")
    public ResponseEntity<DashboardResumenResponse.PropuestaResumen> propuestas() {
        return ResponseEntity.ok(useCase.getPropuestas());
    }
}
