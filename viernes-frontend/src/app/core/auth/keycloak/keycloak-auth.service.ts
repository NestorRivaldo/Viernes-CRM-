import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import type { UsuarioSesion } from '../auth.service';
import { KEYCLOAK_CONFIG } from './keycloak.config';

/**
 * Adaptador OAuth2 / OpenID Connect con Keycloak para Viernes V1.
 *
 * La SPA usa Authorization Code + PKCE S256. Los tokens los administra
 * keycloak-js en memoria; la aplicación no almacena contraseñas ni tokens
 * en localStorage.
 */
@Injectable({
  providedIn: 'root'
})
export class KeycloakAuthService {
  private keycloakInstance: any = null;
  private isAuthSubject = new BehaviorSubject<boolean>(false);
  private userProfile: UsuarioSesion | null = null;
  private refreshTimer: ReturnType<typeof setInterval> | null = null;

  /**
   * Inicializa Keycloak antes de montar las rutas de Angular.
   * Si la URL actual contiene el callback (?code=...), keycloak-js
   * completa el intercambio con PKCE y deja la sesión lista.
   */
  async init(): Promise<boolean> {
    try {
      const keycloakModule = await import('keycloak-js');
      const KeycloakConstructor = keycloakModule.default || keycloakModule;
      this.keycloakInstance = new KeycloakConstructor({
        url: KEYCLOAK_CONFIG.url,
        realm: KEYCLOAK_CONFIG.realm,
        clientId: KEYCLOAK_CONFIG.clientId
      });

      this.keycloakInstance.onTokenExpired = () => {
        void this.refreshToken();
      };

      const authenticated = await this.keycloakInstance.init({
        onLoad: KEYCLOAK_CONFIG.initOptions.onLoad,
        checkLoginIframe: KEYCLOAK_CONFIG.initOptions.checkLoginIframe,
        pkceMethod: KEYCLOAK_CONFIG.initOptions.pkceMethod,
        flow: KEYCLOAK_CONFIG.initOptions.flow,
        scope: KEYCLOAK_CONFIG.initOptions.scope,
        enableLogging: KEYCLOAK_CONFIG.initOptions.enableLogging,
        silentCheckSsoRedirectUri: this.silentCheckSsoUri(),
        redirectUri: this.dashboardUri()
      });

      this.isAuthSubject.next(authenticated);

      if (authenticated) {
        this.loadUserProfileFromToken();
        this.startTokenRefresh();
      }

      return authenticated;
    } catch (error) {
      console.error('[KeycloakAuthService] Error inicializando Keycloak:', error);
      this.keycloakInstance = null;
      this.isAuthSubject.next(false);
      return false;
    }
  }

  /**
   * Redirige al portal de Keycloak manteniendo Authorization Code + PKCE.
   */
  async login(redirectUri?: string): Promise<void> {
    if (!this.keycloakInstance) {
      await this.init();
    }

    if (!this.keycloakInstance) {
      throw new Error('Keycloak no está disponible.');
    }

    await this.keycloakInstance.login({
      redirectUri: redirectUri || this.dashboardUri()
    });
  }

  /**
   * Cierra la sesión de Keycloak y vuelve a la pantalla de acceso.
   */
  async logout(redirectUri?: string): Promise<void> {
    this.clearSession();

    if (!this.keycloakInstance) {
      window.location.href = this.loginUri();
      return;
    }

    await this.keycloakInstance.logout({
      redirectUri: redirectUri || this.loginUri()
    });
  }

  /**
   * Obtiene el token actual, renovándolo si está por expirar.
   */
  async getToken(): Promise<string | null> {
    if (!this.keycloakInstance) return null;

    try {
      await this.keycloakInstance.updateToken(KEYCLOAK_CONFIG.minTokenValiditySeconds);
      return this.keycloakInstance.token || null;
    } catch {
      this.clearSession();
      return null;
    }
  }

  /**
   * Token síncrono para el interceptor HTTP de Angular.
   */
  getTokenSync(): string | null {
    return this.keycloakInstance?.token || null;
  }

  isAuthenticated(): Observable<boolean> {
    return this.isAuthSubject.asObservable();
  }

  isLoggedIn(): boolean {
    return this.keycloakInstance?.authenticated === true && !!this.keycloakInstance?.token;
  }

  getUsuario(): UsuarioSesion | null {
    if (!this.userProfile && this.keycloakInstance?.tokenParsed) {
      this.loadUserProfileFromToken();
    }
    return this.userProfile;
  }

  getRol(): string | null {
    return this.getUsuario()?.rol || null;
  }

  hasRole(...roles: string[]): boolean {
    const tokenRoles = this.getTokenRoles();
    return roles.some((role) => tokenRoles.includes(role.toUpperCase()));
  }

  private getTokenRoles(): string[] {
    const roles = this.keycloakInstance?.tokenParsed?.realm_access?.roles;
    if (Array.isArray(roles)) {
      return roles.filter((role): role is string => typeof role === 'string');
    }
    return this.userProfile?.roles || [];
  }

  private loadUserProfileFromToken(): void {
    const parsedToken = this.keycloakInstance?.tokenParsed;
    if (!parsedToken) return;

    const roles = this.getTokenRoles();
    let rolPrincipal = 'VENDEDOR';
    if (roles.includes('ADMIN')) {
      rolPrincipal = 'ADMIN';
    } else if (roles.includes('JEFE')) {
      rolPrincipal = 'JEFE';
    }

    this.userProfile = {
      id: 1,
      nombre: parsedToken.given_name || parsedToken.name || 'Usuario',
      apellido: parsedToken.family_name || '',
      correo: parsedToken.email || parsedToken.preferred_username || '',
      rol: rolPrincipal,
      roles,
      estado: 'ACTIVO'
    };
  }

  private startTokenRefresh(): void {
    this.stopTokenRefresh();
    this.refreshTimer = setInterval(() => {
      void this.refreshToken();
    }, 20000);
  }

  private stopTokenRefresh(): void {
    if (this.refreshTimer) {
      clearInterval(this.refreshTimer);
      this.refreshTimer = null;
    }
  }

  private async refreshToken(): Promise<void> {
    if (!this.keycloakInstance?.authenticated) return;

    try {
      const refreshed = await this.keycloakInstance.updateToken(KEYCLOAK_CONFIG.minTokenValiditySeconds);
      if (refreshed) {
        this.loadUserProfileFromToken();
      }
    } catch (error) {
      console.warn('[KeycloakAuthService] No se pudo renovar el token:', error);
      this.clearSession();
    }
  }

  private clearSession(): void {
    this.stopTokenRefresh();
    this.userProfile = null;
    this.isAuthSubject.next(false);
  }

  private dashboardUri(): string {
    return `${window.location.origin}/dashboard`;
  }

  private loginUri(): string {
    return `${window.location.origin}/login`;
  }

  private silentCheckSsoUri(): string {
    return `${window.location.origin}/silent-check-sso.html`;
  }
}
