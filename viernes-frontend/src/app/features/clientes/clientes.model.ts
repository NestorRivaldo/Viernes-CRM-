export enum EstadoCliente {
  ACTIVO = 'ACTIVO',
  INACTIVO = 'INACTIVO'
}

export enum TipoDocumento {
  DNI = 'DNI',
  RUC = 'RUC'
}

export interface ClienteResponse {
  id: number;
  nombre: string;
  empresa: string;
  tipoDocumento?: TipoDocumento;
  numeroDocumento?: string;
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
  tipoDocumento?: TipoDocumento;
  numeroDocumento?: string;
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

export interface DocumentoConsultaResponse {
  tipoDocumento: TipoDocumento;
  numeroDocumento: string;
  nombreCompleto?: string | null;
  razonSocial?: string | null;
  nombreComercial?: string | null;
  estado?: string | null;
  condicion?: string | null;
  direccion?: string | null;
  telefonos?: string[];
}
