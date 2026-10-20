import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiService } from '../../core/services/api-service';
import { ProductoRequest, ProductoResponse, EstadoProducto } from './producto.model';

@Injectable({ providedIn: 'root' })
export class ProductosService {
  private path = '/productos';

  constructor(private http: HttpClient, private apiService: ApiService) {}

  listar(q?: string, estado?: EstadoProducto): Observable<ProductoResponse[]> {
    let params = new HttpParams();
    if (q) params = params.set('q', q);
    if (estado) params = params.set('estado', estado);

    return this.http.get<ProductoResponse[]>(this.apiService.buildUrl(this.path), { params });
  }

  obtener(id: number): Observable<ProductoResponse> {
    return this.http.get<ProductoResponse>(this.apiService.buildUrl(`${this.path}/${id}`));
  }

  crear(producto: ProductoRequest): Observable<ProductoResponse> {
    return this.http.post<ProductoResponse>(this.apiService.buildUrl(this.path), producto);
  }

  actualizar(id: number, producto: ProductoRequest): Observable<ProductoResponse> {
    return this.http.put<ProductoResponse>(this.apiService.buildUrl(`${this.path}/${id}`), producto);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(this.apiService.buildUrl(`${this.path}/${id}`));
  }
}
