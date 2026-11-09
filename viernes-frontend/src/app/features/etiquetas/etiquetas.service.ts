import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiService } from '../../core/services/api-service';
import { EtiquetaRequest, EtiquetaResponse } from './etiquetas.model';

@Injectable({ providedIn: 'root' })
export class EtiquetasService {
  private path = '/etiquetas';

  constructor(private http: HttpClient, private apiService: ApiService) {}

  listar(activa?: boolean, q?: string): Observable<EtiquetaResponse[]> {
    let params = new HttpParams();
    if (activa !== undefined && activa !== null) {
      params = params.set('activa', String(activa));
    }
    if (q?.trim()) {
      params = params.set('q', q.trim());
    }
    return this.http.get<EtiquetaResponse[]>(this.apiService.buildUrl(this.path), { params });
  }

  obtener(id: number): Observable<EtiquetaResponse> {
    return this.http.get<EtiquetaResponse>(this.apiService.buildUrl(`${this.path}/${id}`));
  }

  crear(request: EtiquetaRequest): Observable<EtiquetaResponse> {
    return this.http.post<EtiquetaResponse>(this.apiService.buildUrl(this.path), request);
  }

  actualizar(id: number, request: EtiquetaRequest): Observable<EtiquetaResponse> {
    return this.http.put<EtiquetaResponse>(this.apiService.buildUrl(`${this.path}/${id}`), request);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(this.apiService.buildUrl(`${this.path}/${id}`));
  }
}
