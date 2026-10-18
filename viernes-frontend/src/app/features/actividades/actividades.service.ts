import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiService } from '../../core/services/api-service';
import { ActividadRequest, ActividadResponse, EstadoActividad } from './actividades.model';

@Injectable({ providedIn: 'root' })
export class ActividadesService {
  private path = '/actividades';

  constructor(private http: HttpClient, private apiService: ApiService) {}

  listar(filtros?: any): Observable<ActividadResponse[]> {
    let params = new HttpParams();
    if (filtros) {
      if (filtros.estado) params = params.set('estado', filtros.estado);
      if (filtros.idCliente) params = params.set('idCliente', filtros.idCliente);
      if (filtros.idLead) params = params.set('idLead', filtros.idLead);
      if (filtros.idOportunidad) params = params.set('idOportunidad', filtros.idOportunidad);
      if (filtros.vencidas) params = params.set('vencidas', true);
    }
    return this.http.get<ActividadResponse[]>(this.apiService.buildUrl(this.path), { params });
  }

  obtener(id: number): Observable<ActividadResponse> {
    return this.http.get<ActividadResponse>(this.apiService.buildUrl(`${this.path}/${id}`));
  }

  crear(actividad: ActividadRequest): Observable<ActividadResponse> {
    return this.http.post<ActividadResponse>(this.apiService.buildUrl(this.path), actividad);
  }

  actualizar(id: number, actividad: ActividadRequest): Observable<ActividadResponse> {
    return this.http.put<ActividadResponse>(this.apiService.buildUrl(`${this.path}/${id}`), actividad);
  }

  cambiarEstado(id: number, estado: EstadoActividad): Observable<ActividadResponse> {
    let params = new HttpParams().set('estado', estado);
    return this.http.patch<ActividadResponse>(this.apiService.buildUrl(`${this.path}/${id}/estado`), {}, { params });
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(this.apiService.buildUrl(`${this.path}/${id}`));
  }
}
