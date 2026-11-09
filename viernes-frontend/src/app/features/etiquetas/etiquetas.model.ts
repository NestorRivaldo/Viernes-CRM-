export interface EtiquetaResponse {
  id: number;
  nombre: string;
  color: string;
  descripcion?: string | null;
  activa: boolean;
  creadoEn: string;
}

export interface EtiquetaRequest {
  nombre: string;
  color?: string;
  descripcion?: string | null;
  activa?: boolean;
}
