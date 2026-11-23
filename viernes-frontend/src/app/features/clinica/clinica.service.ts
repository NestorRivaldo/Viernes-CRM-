import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiService } from '../../core/services/api-service';
import { Clinica, ConsultorioRequest, ConsultorioResponse, Especialidad } from './clinica.model';

@Injectable({ providedIn: 'root' })
export class ClinicaService {
  constructor(private http: HttpClient, private apiService: ApiService) {}

  // --- CLINICAS ---
  listarClinicas(): Observable<Clinica[]> {
    return this.http.get<Clinica[]>(this.apiService.buildUrl('/clinica/clinicas'));
  }

  obtenerClinica(id: number): Observable<Clinica> {
    return this.http.get<Clinica>(this.apiService.buildUrl(`/clinica/clinicas/${id}`));
  }

  crearClinica(clinica: Clinica): Observable<Clinica> {
    return this.http.post<Clinica>(this.apiService.buildUrl('/clinica/clinicas'), clinica);
  }

  actualizarClinica(id: number, clinica: Clinica): Observable<Clinica> {
    return this.http.put<Clinica>(this.apiService.buildUrl(`/clinica/clinicas/${id}`), clinica);
  }

  eliminarClinica(id: number): Observable<void> {
    return this.http.delete<void>(this.apiService.buildUrl(`/clinica/clinicas/${id}`));
  }

  // --- ESPECIALIDADES ---
  listarEspecialidades(): Observable<Especialidad[]> {
    return this.http.get<Especialidad[]>(this.apiService.buildUrl('/clinica/especialidades'));
  }

  // --- CONSULTORIOS ---
  listarConsultorios(): Observable<ConsultorioResponse[]> {
    return this.http.get<ConsultorioResponse[]>(this.apiService.buildUrl('/clinica/consultorios'));
  }

  obtenerConsultorio(id: number): Observable<ConsultorioResponse> {
    return this.http.get<ConsultorioResponse>(this.apiService.buildUrl(`/clinica/consultorios/${id}`));
  }

  crearConsultorio(consultorio: ConsultorioRequest): Observable<ConsultorioResponse> {
    return this.http.post<ConsultorioResponse>(this.apiService.buildUrl('/clinica/consultorios'), consultorio);
  }

  eliminarConsultorio(id: number): Observable<void> {
    return this.http.delete<void>(this.apiService.buildUrl(`/clinica/consultorios/${id}`));
  }
}
