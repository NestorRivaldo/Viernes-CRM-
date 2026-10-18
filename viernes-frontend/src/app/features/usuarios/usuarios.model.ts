export enum RolUsuario {
  ADMIN = 'ADMIN',
  JEFE = 'JEFE',
  VENDEDOR = 'VENDEDOR'
}

export enum EstadoUsuario {
  ACTIVO = 'ACTIVO',
  INACTIVO = 'INACTIVO'
}

export interface UsuarioResponse {
  id: number;
  nombre: string;
  apellido: string;
  correo: string;
  rol: RolUsuario;
  estado: EstadoUsuario;
  creadoEn: string;
}

export interface UsuarioRequest {
  nombre: string;
  apellido: string;
  correo: string;
  password?: string;
  rol: RolUsuario;
}
