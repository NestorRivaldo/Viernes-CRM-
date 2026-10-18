import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { ApiService } from '../../core/services/api-service';
import { AuthService } from '../../core/auth/auth.service';
import { DashboardResumenResponse } from './dashboard.model';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  constructor(
    private http: HttpClient,
    private apiService: ApiService,
    private authService: AuthService
  ) {}

  getResumen(): Observable<DashboardResumenResponse> {
    if (this.authService.isDevBypass()) {
      return of({
        clientes: { total: 3 },
        leads: { total: 5, porEstado: [] },
        oportunidades: {
          total: 2,
          abiertas: 2,
          ganadas: 0,
          perdidas: 0,
          valorPipeline: 4000,
          valorGanado: 0,
          porEtapa: [
            { etapa: 'PROSPECCION', cantidad: 1, valorTotal: 0 },
            { etapa: 'CONTACTO', cantidad: 1, valorTotal: 4000 }
          ]
        },
        actividades: {
          pendientes: 1,
          enProceso: 0,
          completadas: 2,
          canceladas: 0,
          vencidas: 0,
          porTipo: [],
          porEstado: []
        },
        propuestas: {
          total: 1,
          porEstado: [],
          valorTotalAprobadas: 0
        }
      });
    }

    return this.http.get<DashboardResumenResponse>(this.apiService.buildUrl('/dashboard/resumen'));
  }
}

