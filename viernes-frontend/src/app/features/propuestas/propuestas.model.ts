export enum EstadoPropuesta {
  BORRADOR = 'BORRADOR',
  ENVIADA = 'ENVIADA',
  APROBADA = 'APROBADA',
  RECHAZADA = 'RECHAZADA',
  CANCELADA = 'CANCELADA'
}

export interface DetallePropuestaResponse {
  id: number;
  descripcion: string;
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
}

export interface DetallePropuestaRequest {
  descripcion: string;
  cantidad: number;
  precioUnitario: number;
}

export interface PropuestaResponse {
  id: number;
  numeroPropuesta: string;
  idOportunidad: number;
  idCliente: number;
  clienteNombre: string;
  estado: EstadoPropuesta;
  version: number;
  subtotal: number;
  descuento: number;
  total: number;
  responsable: string;
  fechaEmision: string;
  fechaVencimiento: string;
  observaciones: string;
  creadoEn: string;
  detalles: DetallePropuestaResponse[];
}

export interface PropuestaRequest {
  idOportunidad: number;
  idCliente: number;
  clienteNombre?: string;
  responsable?: string;
  fechaEmision: string;
  fechaVencimiento?: string;
  descuento?: number;
  observaciones?: string;
  detalles: DetallePropuestaRequest[];
}
