import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, catchError, map, of, tap } from 'rxjs';
import { ApiService } from '../services/api-service';

export interface UsuarioSesion {
  id: number;
  nombre: string;
  apellido: string;
  correo: string;
  rol: string;
  estado: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly TOKEN_KEY = 'viernes_jwt';
  private readonly USUARIO_KEY = 'viernes_usuario';
  private isAuthenticatedSubject = new BehaviorSubject<boolean>(this.hasToken());

  constructor(private http: HttpClient, private apiService: ApiService) {}

  login(credentials: any): Observable<any> {
    return this.http.post<any>(this.apiService.buildUrl('/auth/login'), credentials).pipe(
      tap((response) => {
        if (response && response.token) {
          this.setToken(response.token);
          if (response.usuario) {
            this.setUsuario(response.usuario);
          }
        }
      })
    );
  }

  readonly DEV_TOKEN = 'dev-bypass';

  loginSinCredenciales(): Observable<void> {
    return this.login({ correo: 'admin@viernes.com', password: 'Admin12345' }).pipe(
      map(() => void 0),
      catchError(() => {
        this.setToken(this.DEV_TOKEN);
        this.setUsuario({
          id: 0,
          nombre: 'Modo',
          apellido: 'Demo',
          correo: 'demo@viernes.local',
          rol: 'ADMIN',
          estado: 'ACTIVO'
        });
        return of(void 0);
      })
    );
  }

  isDevBypass(): boolean {
    return this.getToken() === this.DEV_TOKEN;
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.USUARIO_KEY);
    this.isAuthenticatedSubject.next(false);
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  setToken(token: string): void {
    localStorage.setItem(this.TOKEN_KEY, token);
    this.isAuthenticatedSubject.next(true);
  }

  hasToken(): boolean {
    return !!this.getToken();
  }

  isAuthenticated(): Observable<boolean> {
    return this.isAuthenticatedSubject.asObservable();
  }

  setUsuario(usuario: UsuarioSesion): void {
    localStorage.setItem(this.USUARIO_KEY, JSON.stringify(usuario));
  }

  getUsuario(): UsuarioSesion | null {
    const raw = localStorage.getItem(this.USUARIO_KEY);
    if (raw) {
      try {
        return JSON.parse(raw) as UsuarioSesion;
      } catch {
        localStorage.removeItem(this.USUARIO_KEY);
      }
    }
    // Respaldo: el JWT solo trae el correo (sub), sin claim de rol
    const payload = this.decodeJwtPayload();
    if (payload?.sub) {
      return { correo: payload.sub } as UsuarioSesion;
    }
    return null;
  }

  getRol(): string | null {
    return this.getUsuario()?.rol ?? null;
  }

  hasRole(...roles: string[]): boolean {
    const rol = this.getRol();
    return !!rol && roles.includes(rol);
  }

  private decodeJwtPayload(): any | null {
    const token = this.getToken();
    if (!token) return null;
    try {
      const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
      return JSON.parse(atob(base64));
    } catch {
      return null;
    }
  }
}
