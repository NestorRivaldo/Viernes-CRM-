export enum EstadoCliente {
  ACTIVO = 'ACTIVO',
  INACTIVO = 'INACTIVO'
}

export interface ClienteResponse {
  id: number;
  nombre: string;
  empresa: string;
  correo: string;
  telefono: string;
  estado: EstadoCliente;
  responsable: string;
  creadoEn: string;
  contactos?: any[];
}

export interface ClienteRequest {
  nombre: string;
  empresa?: string;
  correo: string;
  telefono?: string;
  estado: EstadoCliente;
  responsable?: string;
}

export interface ContactoResponse {
  id: number;
  idCliente: number;
  nombre: string;
  apellido: string;
  cargo: string;
  correo: string;
  telefono: string;
  principal: boolean;
  creadoEn: string;
}

export interface ContactoRequest {
  nombre: string;
  apellido?: string;
  cargo?: string;
  correo?: string;
  telefono?: string;
  principal?: boolean;
}
