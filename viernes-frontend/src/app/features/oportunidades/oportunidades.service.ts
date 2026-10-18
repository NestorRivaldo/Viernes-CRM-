import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiService } from '../../core/services/api-service';
import { OportunidadRequest, OportunidadResponse, EtapaOportunidad } from './oportunidades.model';

@Injectable({ providedIn: 'root' })
export class OportunidadesService {
  private path = '/oportunidades';

  constructor(private http: HttpClient, private apiService: ApiService) {}

  listar(q?: string, etapa?: EtapaOportunidad): Observable<OportunidadResponse[]> {
    let params = new HttpParams();
    if (q) params = params.set('q', q);
    if (etapa) params = params.set('etapa', etapa);
    
    return this.http.get<OportunidadResponse[]>(this.apiService.buildUrl(this.path), { params });
  }

  obtener(id: number): Observable<OportunidadResponse> {
    return this.http.get<OportunidadResponse>(this.apiService.buildUrl(`${this.path}/${id}`));
  }

  crear(oportunidad: OportunidadRequest): Observable<OportunidadResponse> {
    return this.http.post<OportunidadResponse>(this.apiService.buildUrl(this.path), oportunidad);
  }

  actualizar(id: number, oportunidad: OportunidadRequest): Observable<OportunidadResponse> {
    return this.http.put<OportunidadResponse>(this.apiService.buildUrl(`${this.path}/${id}`), oportunidad);
  }

  cambiarEtapa(id: number, etapa: EtapaOportunidad): Observable<OportunidadResponse> {
    let params = new HttpParams().set('etapa', etapa);
    return this.http.patch<OportunidadResponse>(this.apiService.buildUrl(`${this.path}/${id}/etapa`), {}, { params });
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(this.apiService.buildUrl(`${this.path}/${id}`));
  }
}
