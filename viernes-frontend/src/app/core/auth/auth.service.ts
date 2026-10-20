import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpHeaders } from '@angular/common/http';
import { BehaviorSubject, Observable, catchError, map, tap, throwError } from 'rxjs';
import { KEYCLOAK_CONFIG } from './keycloak/keycloak.config';

export interface UsuarioSesion {
  id: number;
  nombre: string;
  apellido: string;
  correo: string;
  rol: string;
  roles?: string[];
  estado: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly TOKEN_KEY = 'viernes_jwt';
  private readonly REFRESH_TOKEN_KEY = 'viernes_refresh_jwt';
  private readonly USUARIO_KEY = 'viernes_usuario';
  private isAuthenticatedSubject = new BehaviorSubject<boolean>(this.hasToken());

  constructor(private http: HttpClient) {
    this.handleRedirectCallback();
  }

  /**
   * Autenticación contra Keycloak mediante OAuth2 Direct Grant (Resource Owner Password Credentials).
   */
  login(credentials: { correo: string; password: string }): Observable<any> {
    const tokenUrl = `${KEYCLOAK_CONFIG.url}/realms/${KEYCLOAK_CONFIG.realm}/protocol/openid-connect/token`;

    const body = new HttpParams()
      .set('client_id', KEYCLOAK_CONFIG.clientId)
      .set('grant_type', 'password')
      .set('username', credentials.correo ? credentials.correo.trim() : '')
      .set('password', credentials.password || '')
      .set('scope', 'openid profile email');

    const headers = new HttpHeaders({
      'Content-Type': 'application/x-www-form-urlencoded'
    });

    return this.http.post<any>(tokenUrl, body.toString(), { headers }).pipe(
      tap((response) => {
        if (response && response.access_token) {
          this.setToken(response.access_token);
          if (response.refresh_token) {
            localStorage.setItem(this.REFRESH_TOKEN_KEY, response.refresh_token);
          }

          const usuario = this.decodeTokenToUsuario(response.access_token, credentials.correo);
          this.setUsuario(usuario);
        }
      }),
      catchError((error) => {
        let msg = 'Credenciales inválidas en Keycloak.';
        if (error.status === 0) {
          msg = 'No se pudo conectar con el servidor Keycloak (http://localhost:8180).';
        } else if (error.error?.error_description) {
          msg = error.error.error_description;
        }
        return throwError(() => ({ friendlyMessage: msg, original: error }));
      })
    );
  }

  /**
   * Redirige al portal oficial de Keycloak para inicio de sesión OIDC estándar.
   */
  loginWithKeycloakSSO(redirectUri?: string): void {
    const callback = redirectUri || window.location.origin + '/dashboard';
    const authUrl = `${KEYCLOAK_CONFIG.url}/realms/${KEYCLOAK_CONFIG.realm}/protocol/openid-connect/auth?client_id=${KEYCLOAK_CONFIG.clientId}&response_type=code&scope=openid%20profile%20email&redirect_uri=${encodeURIComponent(callback)}`;
    window.location.href = authUrl;
  }

  /**
   * Si la URL contiene ?code= (redirección desde Keycloak SSO), intercambia el código por tokens.
   */
  private handleRedirectCallback(): void {
    if (typeof window === 'undefined') return;
    const urlParams = new URLSearchParams(window.location.search);
    const code = urlParams.get('code');
    if (code) {
      const tokenUrl = `${KEYCLOAK_CONFIG.url}/realms/${KEYCLOAK_CONFIG.realm}/protocol/openid-connect/token`;
      const redirectUri = window.location.origin + window.location.pathname;

      const body = new HttpParams()
        .set('client_id', KEYCLOAK_CONFIG.clientId)
        .set('grant_type', 'authorization_code')
        .set('code', code)
        .set('redirect_uri', redirectUri);

      const headers = new HttpHeaders({
        'Content-Type': 'application/x-www-form-urlencoded'
      });

      this.http.post<any>(tokenUrl, body.toString(), { headers }).subscribe({
        next: (response) => {
          if (response.access_token) {
            this.setToken(response.access_token);
            if (response.refresh_token) {
              localStorage.setItem(this.REFRESH_TOKEN_KEY, response.refresh_token);
            }
            const usuario = this.decodeTokenToUsuario(response.access_token, 'admin@viernes.com');
            this.setUsuario(usuario);
            window.history.replaceState({}, document.title, window.location.pathname);
            window.location.href = '/dashboard';
          }
        },
        error: (err) => {
          console.error('[AuthService] Error procesando callback de Keycloak:', err);
        }
      });
    }
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.REFRESH_TOKEN_KEY);
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
    const token = this.getToken();
    if (!token) return false;
    try {
      const payload = this.parseJwt(token);
      const now = Math.floor(Date.now() / 1000);
      if (payload.exp && payload.exp < now) {
        this.logout();
        return false;
      }
      return true;
    } catch {
      return false;
    }
  }

  isAuthenticated(): Observable<boolean> {
    return this.isAuthenticatedSubject.asObservable();
  }

  getUsuario(): UsuarioSesion | null {
    const raw = localStorage.getItem(this.USUARIO_KEY);
    if (!raw) return null;
    try {
      return JSON.parse(raw);
    } catch {
      return null;
    }
  }

  setUsuario(usuario: UsuarioSesion): void {
    localStorage.setItem(this.USUARIO_KEY, JSON.stringify(usuario));
  }

  hasRole(...roles: string[]): boolean {
    const usuario = this.getUsuario();
    if (!usuario) return false;
    const userRoles = usuario.roles || (usuario.rol ? [usuario.rol] : []);
    return roles.some((r) => userRoles.includes(r.toUpperCase()));
  }

  private decodeTokenToUsuario(token: string, fallbackEmail: string): UsuarioSesion {
    try {
      const payload = this.parseJwt(token);
      const roles: string[] = payload.realm_access?.roles || [];
      let rolPrincipal = 'VENDEDOR';
      if (roles.includes('ADMIN')) rolPrincipal = 'ADMIN';
      else if (roles.includes('JEFE')) rolPrincipal = 'JEFE';

      return {
        id: 1,
        nombre: payload.given_name || payload.name || 'Admin',
        apellido: payload.family_name || '',
        correo: payload.email || payload.preferred_username || fallbackEmail,
        rol: rolPrincipal,
        roles: roles,
        estado: 'ACTIVO'
      };
    } catch {
      return {
        id: 1,
        nombre: 'Admin',
        apellido: 'Viernes',
        correo: fallbackEmail,
        rol: 'ADMIN',
        roles: ['ADMIN', 'JEFE', 'VENDEDOR'],
        estado: 'ACTIVO'
      };
    }
  }

  private parseJwt(token: string): any {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );
    return JSON.parse(jsonPayload);
  }
}
