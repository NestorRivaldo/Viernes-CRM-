import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiService } from '../../core/services/api-service';
import { PropuestaRequest, PropuestaResponse, EstadoPropuesta } from './propuestas.model';

@Injectable({ providedIn: 'root' })
export class PropuestasService {
  private path = '/propuestas';

  constructor(private http: HttpClient, private apiService: ApiService) {}

  listar(idOportunidad?: number, idCliente?: number, estado?: EstadoPropuesta): Observable<PropuestaResponse[]> {
    let params = new HttpParams();
    if (idOportunidad) params = params.set('idOportunidad', idOportunidad);
    if (idCliente) params = params.set('idCliente', idCliente);
    if (estado) params = params.set('estado', estado);
    
    return this.http.get<PropuestaResponse[]>(this.apiService.buildUrl(this.path), { params });
  }

  obtener(id: number): Observable<PropuestaResponse> {
    return this.http.get<PropuestaResponse>(this.apiService.buildUrl(`${this.path}/${id}`));
  }

  crear(propuesta: PropuestaRequest): Observable<PropuestaResponse> {
    return this.http.post<PropuestaResponse>(this.apiService.buildUrl(this.path), propuesta);
  }

  actualizar(id: number, propuesta: PropuestaRequest): Observable<PropuestaResponse> {
    return this.http.put<PropuestaResponse>(this.apiService.buildUrl(`${this.path}/${id}`), propuesta);
  }

  enviar(id: number): Observable<PropuestaResponse> {
    return this.http.patch<PropuestaResponse>(this.apiService.buildUrl(`${this.path}/${id}/enviar`), {});
  }

  aprobar(id: number): Observable<PropuestaResponse> {
    return this.http.patch<PropuestaResponse>(this.apiService.buildUrl(`${this.path}/${id}/aprobar`), {});
  }

  rechazar(id: number): Observable<PropuestaResponse> {
    return this.http.patch<PropuestaResponse>(this.apiService.buildUrl(`${this.path}/${id}/rechazar`), {});
  }

  cancelar(id: number): Observable<PropuestaResponse> {
    return this.http.patch<PropuestaResponse>(this.apiService.buildUrl(`${this.path}/${id}/cancelar`), {});
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(this.apiService.buildUrl(`${this.path}/${id}`));
  }
}
