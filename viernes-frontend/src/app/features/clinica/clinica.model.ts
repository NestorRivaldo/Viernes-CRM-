export interface Clinica {
  id?: number;
  ruc: string;
  razonSocial: string;
  telefono?: string;
  activo?: boolean;
}

export type EstadoConsultorio = 'DISPONIBLE' | 'OCUPADO' | 'MANTENIMIENTO';

export interface Especialidad {
  id: number;
  codigo: string;
  nombre: string;
}

export interface ConsultorioRequest {
  idClinica: number;
  codigo: string;
  tarifaHora: number;
  estado: EstadoConsultorio;
  especialidadIds: number[];
}

export interface ConsultorioResponse {
  id: number;
  idClinica: number;
  clinicaRazonSocial: string;
  codigo: string;
  tarifaHora: number;
  estado: EstadoConsultorio;
  especialidades: Especialidad[];
}
