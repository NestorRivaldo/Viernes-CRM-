import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiService } from '../../core/services/api-service';
import { ClienteRequest, ClienteResponse, ContactoRequest, ContactoResponse, EstadoCliente } from './clientes.model';

@Injectable({ providedIn: 'root' })
export class ClientesService {
  private path = '/clientes';

  constructor(private http: HttpClient, private apiService: ApiService) {}

  listar(q?: string, estado?: EstadoCliente): Observable<ClienteResponse[]> {
    let params = new HttpParams();
    if (q) params = params.set('q', q);
    if (estado) params = params.set('estado', estado);
    
    return this.http.get<ClienteResponse[]>(this.apiService.buildUrl(this.path), { params });
  }

  obtener(id: number): Observable<ClienteResponse> {
    return this.http.get<ClienteResponse>(this.apiService.buildUrl(`${this.path}/${id}`));
  }

  crear(cliente: ClienteRequest): Observable<ClienteResponse> {
    return this.http.post<ClienteResponse>(this.apiService.buildUrl(this.path), cliente);
  }

  actualizar(id: number, cliente: ClienteRequest): Observable<ClienteResponse> {
    return this.http.put<ClienteResponse>(this.apiService.buildUrl(`${this.path}/${id}`), cliente);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(this.apiService.buildUrl(`${this.path}/${id}`));
  }

  desactivar(id: number): Observable<void> {
    return this.http.patch<void>(this.apiService.buildUrl(`${this.path}/${id}/desactivar`), {});
  }

  listarContactos(clienteId: number): Observable<ContactoResponse[]> {
    return this.http.get<ContactoResponse[]>(this.apiService.buildUrl(`${this.path}/${clienteId}/contactos`));
  }

  agregarContacto(clienteId: number, contacto: ContactoRequest): Observable<ContactoResponse> {
    return this.http.post<ContactoResponse>(this.apiService.buildUrl(`${this.path}/${clienteId}/contactos`), contacto);
  }

  actualizarContacto(clienteId: number, contactoId: number, contacto: ContactoRequest): Observable<ContactoResponse> {
    return this.http.put<ContactoResponse>(this.apiService.buildUrl(`${this.path}/${clienteId}/contactos/${contactoId}`), contacto);
  }

  eliminarContacto(clienteId: number, contactoId: number): Observable<void> {
    return this.http.delete<void>(this.apiService.buildUrl(`${this.path}/${clienteId}/contactos/${contactoId}`));
  }
}
