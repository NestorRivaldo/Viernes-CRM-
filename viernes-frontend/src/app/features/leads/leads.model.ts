export enum EstadoLead {
  NUEVO = 'NUEVO',
  CONTACTADO = 'CONTACTADO',
  CALIFICADO = 'CALIFICADO',
  CONVERTIDO = 'CONVERTIDO',
  DESCARTADO = 'DESCARTADO'
}

export interface LeadResponse {
  id: number;
  nombre: string;
  empresa: string;
  correo: string;
  telefono: string;
  fuente: string;
  estado: EstadoLead;
  responsable: string;
  fecha: string;
  creadoEn: string;
}

export interface LeadRequest {
  nombre: string;
  empresa?: string;
  correo: string;
  telefono?: string;
  fuente?: string;
  estado: EstadoLead;
  responsable?: string;
  fecha?: string;
}
