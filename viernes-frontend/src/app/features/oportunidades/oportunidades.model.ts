export enum EtapaOportunidad {
  PROSPECCION = 'PROSPECCION',
  CONTACTO = 'CONTACTO',
  CALIFICACION = 'CALIFICACION',
  PROPUESTA = 'PROPUESTA',
  NEGOCIACION = 'NEGOCIACION',
  GANADA = 'GANADA',
  PERDIDA = 'PERDIDA'
}

export interface OportunidadResponse {
  id: number;
  nombre: string;
  clienteNombre: string;
  valor: number;
  probabilidad: number;
  valorPonderado: number;
  etapa: EtapaOportunidad;
  responsable: string;
  fechaCierre: string;
  descripcion?: string | null;
  creadoEn: string;
}

export interface OportunidadRequest {
  nombre: string;
  clienteNombre?: string;
  valor?: number;
  probabilidad?: number;
  etapa?: EtapaOportunidad;
  responsable?: string;
  fechaCierre?: string;
  descripcion?: string | null;
}

/* --- Relaciones N:M de la oportunidad --- */

export interface OportunidadContactoResponse {
  id: number;
  idOportunidad: number;
  contactoId: number;
  nombreContacto: string;
  correoContacto: string;
  rol: string | null;
  esDecisor: boolean;
  creadoEn: string;
}

export interface OportunidadContactoRequest {
  contactoId: number;
  rol?: string | null;
  esDecisor?: boolean;
}

export interface OportunidadContactoUpdateRequest {
  rol?: string | null;
  esDecisor?: boolean;
}

export interface OportunidadProductoResponse {
  id: number;
  idOportunidad: number;
  productoId: number;
  codigo: string;
  nombreProducto: string;
  precioBase: number;
  cantidad: number;
  creadoEn: string;
}

export interface OportunidadProductoRequest {
  productoId: number;
  cantidad?: number;
}

export interface OportunidadColaboradorResponse {
  id: number;
  idOportunidad: number;
  usuarioId: number;
  nombreCompleto: string;
  correo: string;
  creadoEn: string;
}

export interface OportunidadColaboradorRequest {
  usuarioId: number;
}
