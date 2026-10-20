import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiService } from '../../core/services/api-service';
import { DashboardResumenResponse } from './dashboard.model';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  constructor(
    private http: HttpClient,
    private apiService: ApiService
  ) {}

  getResumen(): Observable<DashboardResumenResponse> {
    return this.http.get<DashboardResumenResponse>(this.apiService.buildUrl('/dashboard/resumen'));
  }
}
