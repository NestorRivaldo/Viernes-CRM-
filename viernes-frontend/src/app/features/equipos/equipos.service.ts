import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiService } from '../../core/services/api-service';
import { EquipoRequest, EquipoResponse, MiembroRequest, MiembroResponse } from './equipo.model';

@Injectable({ providedIn: 'root' })
export class EquiposService {
  private path = '/equipos';

  constructor(private http: HttpClient, private apiService: ApiService) {}

  listar(activo?: boolean): Observable<EquipoResponse[]> {
    let params = new HttpParams();
    if (activo !== undefined && activo !== null) {
      params = params.set('activo', activo);
    }
    return this.http.get<EquipoResponse[]>(this.apiService.buildUrl(this.path), { params });
  }

  obtener(id: number): Observable<EquipoResponse> {
    return this.http.get<EquipoResponse>(this.apiService.buildUrl(`${this.path}/${id}`));
  }

  crear(equipo: EquipoRequest): Observable<EquipoResponse> {
    return this.http.post<EquipoResponse>(this.apiService.buildUrl(this.path), equipo);
  }

  actualizar(id: number, equipo: EquipoRequest): Observable<EquipoResponse> {
    return this.http.put<EquipoResponse>(this.apiService.buildUrl(`${this.path}/${id}`), equipo);
  }

  activar(id: number): Observable<void> {
    return this.http.patch<void>(this.apiService.buildUrl(`${this.path}/${id}/activar`), {});
  }

  desactivar(id: number): Observable<void> {
    return this.http.patch<void>(this.apiService.buildUrl(`${this.path}/${id}/desactivar`), {});
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(this.apiService.buildUrl(`${this.path}/${id}`));
  }

  listarMiembros(id: number): Observable<MiembroResponse[]> {
    return this.http.get<MiembroResponse[]>(this.apiService.buildUrl(`${this.path}/${id}/miembros`));
  }

  agregarMiembro(id: number, usuarioId: number): Observable<MiembroResponse> {
    const request: MiembroRequest = { usuarioId };
    return this.http.post<MiembroResponse>(this.apiService.buildUrl(`${this.path}/${id}/miembros`), request);
  }

  quitarMiembro(id: number, usuarioId: number): Observable<void> {
    return this.http.delete<void>(this.apiService.buildUrl(`${this.path}/${id}/miembros/${usuarioId}`));
  }
}
