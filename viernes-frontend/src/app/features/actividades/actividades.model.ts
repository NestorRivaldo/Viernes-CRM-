import { EtiquetaResponse } from '../etiquetas/etiquetas.model';

export enum EstadoActividad {
  PENDIENTE = 'PENDIENTE',
  EN_PROCESO = 'EN_PROCESO',
  COMPLETADA = 'COMPLETADA',
  CANCELADA = 'CANCELADA'
}

export enum TipoActividad {
  LLAMADA = 'LLAMADA',
  REUNION = 'REUNION',
  TAREA = 'TAREA',
  SEGUIMIENTO = 'SEGUIMIENTO'
}

export interface ActividadResponse {
  id: number;
  idUsuario: string;
  idCliente: number;
  idLead: number;
  idOportunidad: number;
  tipo: TipoActividad;
  titulo: string;
  descripcion: string;
  fecha: string;
  estado: EstadoActividad;
  creadoEn: string;
  vencida: boolean;
  etiquetas: EtiquetaResponse[];
}

export interface ActividadRequest {
  idUsuario?: string;
  idCliente?: number;
  idLead?: number;
  idOportunidad?: number;
  tipo: TipoActividad;
  titulo: string;
  descripcion?: string;
  fecha: string;
  estado?: EstadoActividad;
  etiquetaIds?: number[];
}
