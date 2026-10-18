import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiService } from '../../core/services/api-service';
import { UsuarioRequest, UsuarioResponse } from './usuarios.model';

@Injectable({ providedIn: 'root' })
export class UsuariosService {
  private path = '/usuarios';

  constructor(private http: HttpClient, private apiService: ApiService) {}

  listar(): Observable<UsuarioResponse[]> {
    return this.http.get<UsuarioResponse[]>(this.apiService.buildUrl(this.path));
  }

  obtener(id: number): Observable<UsuarioResponse> {
    return this.http.get<UsuarioResponse>(this.apiService.buildUrl(`${this.path}/${id}`));
  }

  crear(usuario: UsuarioRequest): Observable<UsuarioResponse> {
    return this.http.post<UsuarioResponse>(this.apiService.buildUrl(this.path), usuario);
  }

  actualizar(id: number, usuario: UsuarioRequest): Observable<UsuarioResponse> {
    return this.http.put<UsuarioResponse>(this.apiService.buildUrl(`${this.path}/${id}`), usuario);
  }

  activar(id: number): Observable<void> {
    return this.http.patch<void>(this.apiService.buildUrl(`${this.path}/${id}/activar`), {});
  }

  desactivar(id: number): Observable<void> {
    return this.http.patch<void>(this.apiService.buildUrl(`${this.path}/${id}/desactivar`), {});
  }
}
