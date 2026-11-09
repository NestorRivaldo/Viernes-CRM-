export interface MensajeResponse {
  id: number;
  usuarioCorreo: string;
  usuarioNombre: string;
  contenido: string;
  creadoEn: string;
}

export interface MensajeRequest {
  contenido: string;
}

export interface ResumenMensajesResponse {
  total: number;
  ultimoId: number | null;
}
