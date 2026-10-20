export enum EstadoProducto {
  ACTIVO = 'ACTIVO',
  INACTIVO = 'INACTIVO',
  DESCONTINUADO = 'DESCONTINUADO'
}

export interface ProductoResponse {
  id: number;
  codigo: string;
  nombre: string;
  descripcion: string;
  categoria: string;
  precioBase: number;
  estado: EstadoProducto;
  creadoEn: string;
}

export interface ProductoRequest {
  codigo: string;
  nombre: string;
  descripcion?: string;
  categoria?: string;
  precioBase: number;
  estado: EstadoProducto;
}
