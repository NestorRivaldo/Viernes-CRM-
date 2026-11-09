export interface EquipoResponse {
  id: number;
  nombre: string;
  descripcion: string;
  activo: boolean;
  creadoEn: string;
  totalMiembros: number;
}

export interface EquipoRequest {
  nombre: string;
  descripcion?: string;
}

export interface MiembroResponse {
  usuarioId: number;
  nombreCompleto: string;
  correo: string;
  agregadoEn: string;
}

export interface MiembroRequest {
  usuarioId: number;
}
