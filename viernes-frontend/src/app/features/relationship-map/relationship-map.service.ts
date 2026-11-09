import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiService } from '../../core/services/api-service';
import { GraphNodeDto, RelationshipMapResponse } from './relationship-map.model';

@Injectable({ providedIn: 'root' })
export class RelationshipMapService {
  private http = inject(HttpClient);
  private apiService = inject(ApiService);
  private path = '/relationship-map';

  obtenerMapaCliente(clienteId: number): Observable<RelationshipMapResponse> {
    return this.http.get<RelationshipMapResponse>(
      this.apiService.buildUrl(`${this.path}/clientes/${clienteId}`)
    );
  }

  obtenerMapaOportunidad(oportunidadId: number): Observable<RelationshipMapResponse> {
    return this.http.get<RelationshipMapResponse>(
      this.apiService.buildUrl(`${this.path}/oportunidades/${oportunidadId}`)
    );
  }

  expandirConexiones(type: string, id: number): Observable<RelationshipMapResponse> {
    return this.http.get<RelationshipMapResponse>(
      this.apiService.buildUrl(`${this.path}/nodes/${type.toLowerCase()}/${id}/connections`)
    );
  }

  obtenerDetalleNodo(type: string, id: number): Observable<GraphNodeDto> {
    return this.http.get<GraphNodeDto>(
      this.apiService.buildUrl(`${this.path}/nodes/${type.toLowerCase()}/${id}`)
    );
  }
}
