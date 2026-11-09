import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiService } from '../../core/services/api-service';
import {
  OportunidadRequest,
  OportunidadResponse,
  EtapaOportunidad,
  OportunidadContactoRequest,
  OportunidadContactoResponse,
  OportunidadContactoUpdateRequest,
  OportunidadProductoRequest,
  OportunidadProductoResponse,
  OportunidadColaboradorRequest,
  OportunidadColaboradorResponse,
} from './oportunidades.model';

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

  /* --- N:M: contactos participantes --- */

  listarContactos(idOportunidad: number): Observable<OportunidadContactoResponse[]> {
    return this.http.get<OportunidadContactoResponse[]>(
      this.apiService.buildUrl(`${this.path}/${idOportunidad}/contactos`)
    );
  }

  agregarContacto(idOportunidad: number, request: OportunidadContactoRequest): Observable<OportunidadContactoResponse> {
    return this.http.post<OportunidadContactoResponse>(
      this.apiService.buildUrl(`${this.path}/${idOportunidad}/contactos`),
      request
    );
  }

  actualizarContacto(
    idOportunidad: number,
    contactoId: number,
    request: OportunidadContactoUpdateRequest
  ): Observable<OportunidadContactoResponse> {
    return this.http.put<OportunidadContactoResponse>(
      this.apiService.buildUrl(`${this.path}/${idOportunidad}/contactos/${contactoId}`),
      request
    );
  }

  quitarContacto(idOportunidad: number, contactoId: number): Observable<void> {
    return this.http.delete<void>(
      this.apiService.buildUrl(`${this.path}/${idOportunidad}/contactos/${contactoId}`)
    );
  }

  /* --- N:M: productos de la oportunidad --- */

  listarProductos(idOportunidad: number): Observable<OportunidadProductoResponse[]> {
    return this.http.get<OportunidadProductoResponse[]>(
      this.apiService.buildUrl(`${this.path}/${idOportunidad}/productos`)
    );
  }

  agregarProducto(idOportunidad: number, request: OportunidadProductoRequest): Observable<OportunidadProductoResponse> {
    return this.http.post<OportunidadProductoResponse>(
      this.apiService.buildUrl(`${this.path}/${idOportunidad}/productos`),
      request
    );
  }

  quitarProducto(idOportunidad: number, productoId: number): Observable<void> {
    return this.http.delete<void>(
      this.apiService.buildUrl(`${this.path}/${idOportunidad}/productos/${productoId}`)
    );
  }

  /* --- N:M: colaboradores --- */

  listarColaboradores(idOportunidad: number): Observable<OportunidadColaboradorResponse[]> {
    return this.http.get<OportunidadColaboradorResponse[]>(
      this.apiService.buildUrl(`${this.path}/${idOportunidad}/colaboradores`)
    );
  }

  agregarColaborador(
    idOportunidad: number,
    request: OportunidadColaboradorRequest
  ): Observable<OportunidadColaboradorResponse> {
    return this.http.post<OportunidadColaboradorResponse>(
      this.apiService.buildUrl(`${this.path}/${idOportunidad}/colaboradores`),
      request
    );
  }

  quitarColaborador(idOportunidad: number, usuarioId: number): Observable<void> {
    return this.http.delete<void>(
      this.apiService.buildUrl(`${this.path}/${idOportunidad}/colaboradores/${usuarioId}`)
    );
  }
}
