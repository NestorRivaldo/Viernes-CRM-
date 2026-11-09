import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { KeycloakAuthService } from './keycloak/keycloak-auth.service';

export interface UsuarioSesion {
  id: number;
  nombre: string;
  apellido: string;
  correo: string;
  rol: string;
  roles?: string[];
  estado: string;
}

/**
 * Fachada de autenticación de Viernes.
 *
 * Mantiene la interfaz que consumen guards, layout y componentes,
 * pero delega toda la sesión a Keycloak (Authorization Code + PKCE).
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly keycloak = inject(KeycloakAuthService);

  login(redirectUri?: string): Promise<void> {
    return this.keycloak.login(redirectUri);
  }

  loginWithKeycloakSSO(redirectUri?: string): Promise<void> {
    return this.login(redirectUri);
  }

  logout(redirectUri?: string): Promise<void> {
    return this.keycloak.logout(redirectUri);
  }

  getToken(): string | null {
    return this.keycloak.getTokenSync();
  }

  getTokenAsync(): Promise<string | null> {
    return this.keycloak.getToken();
  }

  hasToken(): boolean {
    return this.keycloak.isLoggedIn();
  }

  isAuthenticated(): Observable<boolean> {
    return this.keycloak.isAuthenticated();
  }

  getUsuario(): UsuarioSesion | null {
    return this.keycloak.getUsuario();
  }

  hasRole(...roles: string[]): boolean {
    return this.keycloak.hasRole(...roles);
  }
}
