import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiService } from '../../core/services/api-service';
import { LeadRequest, LeadResponse, EstadoLead } from './leads.model';

@Injectable({ providedIn: 'root' })
export class LeadsService {
  private path = '/leads';

  constructor(private http: HttpClient, private apiService: ApiService) {}

  listar(q?: string, estado?: EstadoLead): Observable<LeadResponse[]> {
    let params = new HttpParams();
    if (q) params = params.set('q', q);
    if (estado) params = params.set('estado', estado);
    
    return this.http.get<LeadResponse[]>(this.apiService.buildUrl(this.path), { params });
  }

  obtener(id: number): Observable<LeadResponse> {
    return this.http.get<LeadResponse>(this.apiService.buildUrl(`${this.path}/${id}`));
  }

  crear(lead: LeadRequest): Observable<LeadResponse> {
    return this.http.post<LeadResponse>(this.apiService.buildUrl(this.path), lead);
  }

  actualizar(id: number, lead: LeadRequest): Observable<LeadResponse> {
    return this.http.put<LeadResponse>(this.apiService.buildUrl(`${this.path}/${id}`), lead);
  }

  cambiarEstado(id: number, estado: EstadoLead): Observable<LeadResponse> {
    let params = new HttpParams().set('estado', estado);
    return this.http.patch<LeadResponse>(this.apiService.buildUrl(`${this.path}/${id}/estado`), {}, { params });
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(this.apiService.buildUrl(`${this.path}/${id}`));
  }
}
