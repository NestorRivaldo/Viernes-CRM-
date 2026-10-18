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
}
