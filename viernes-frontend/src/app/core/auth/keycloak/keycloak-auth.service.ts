import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { UsuarioSesion } from '../auth.service';
import { KEYCLOAK_CONFIG } from './keycloak.config';

/**
 * Adaptador de autenticación OAuth2 / OIDC con Keycloak para Viernes V1.
 * Ofrece la misma interfaz que AuthService, permitiendo una transición transparente.
 */
@Injectable({
  providedIn: 'root'
})
export class KeycloakAuthService {
  private keycloakInstance: any = null;
  private isAuthSubject = new BehaviorSubject<boolean>(false);
  private userProfile: UsuarioSesion | null = null;

  constructor() {}

  /**
   * Inicializa la instancia de Keycloak (llamado típicamente en APP_INITIALIZER).
   */
  async init(): Promise<boolean> {
    try {
      // Carga dinámica de keycloak-js para no forzar instalación en tiempo de build si aún no se activa
      let KeycloakConstructor: any;
      try {
        const keycloakModule = await import('keycloak-js');
        KeycloakConstructor = keycloakModule.default || keycloakModule;
      } catch {
        console.warn('[KeycloakAuthService] Paquete keycloak-js no instalado aún. Ejecute: npm install keycloak-js');
        return false;
      }

      this.keycloakInstance = new KeycloakConstructor({
        url: KEYCLOAK_CONFIG.url,
        realm: KEYCLOAK_CONFIG.realm,
        clientId: KEYCLOAK_CONFIG.clientId
      });

      const authenticated = await this.keycloakInstance.init({
        onLoad: KEYCLOAK_CONFIG.initOptions.onLoad,
        checkLoginIframe: KEYCLOAK_CONFIG.initOptions.checkLoginIframe,
        pkceMethod: KEYCLOAK_CONFIG.initOptions.pkceMethod
      });

      this.isAuthSubject.next(authenticated);

      if (authenticated) {
        await this.loadUserProfile();
        this.setupTokenRefresh();
      }

      return authenticated;
    } catch (error) {
      console.error('[KeycloakAuthService] Error inicializando Keycloak:', error);
      this.isAuthSubject.next(false);
      return false;
    }
  }

  /**
   * Redirige al login de Keycloak.
   */
  login(redirectUri?: string): Promise<void> {
    if (!this.keycloakInstance) {
      window.location.href = `${KEYCLOAK_CONFIG.url}/realms/${KEYCLOAK_CONFIG.realm}/protocol/openid-connect/auth?client_id=${KEYCLOAK_CONFIG.clientId}&response_type=code&redirect_uri=${encodeURIComponent(redirectUri || window.location.origin + '/dashboard')}`;
      return Promise.resolve();
    }
    return this.keycloakInstance.login({
      redirectUri: redirectUri || window.location.origin + '/dashboard'
    });
  }

  /**
   * Cierra la sesión en Keycloak y limpia el almacenamiento local.
   */
  logout(redirectUri?: string): Promise<void> {
    this.userProfile = null;
    this.isAuthSubject.next(false);
    if (!this.keycloakInstance) {
      window.location.href = window.location.origin + '/login';
      return Promise.resolve();
    }
    return this.keycloakInstance.logout({
      redirectUri: redirectUri || window.location.origin + '/login'
    });
  }

  /**
   * Obtiene el token de acceso actual o lo renueva si está cerca de expirar.
   */
  async getToken(): Promise<string | null> {
    if (!this.keycloakInstance) return null;
    try {
      await this.keycloakInstance.updateToken(KEYCLOAK_CONFIG.minTokenValiditySeconds);
      return this.keycloakInstance.token || null;
    } catch {
      return null;
    }
  }

  /**
   * Retorna el token de forma síncrona para interceptores simples.
   */
  getTokenSync(): string | null {
    return this.keycloakInstance?.token || null;
  }

  isAuthenticated(): Observable<boolean> {
    return this.isAuthSubject.asObservable();
  }

  isLoggedIn(): boolean {
    return this.isAuthSubject.value;
  }

  getUsuario(): UsuarioSesion | null {
    return this.userProfile;
  }

  getRol(): string | null {
    return this.userProfile?.rol || null;
  }

  hasRole(...roles: string[]): boolean {
    if (!this.keycloakInstance) return false;
    return roles.some(role => this.keycloakInstance.hasRealmRole(role));
  }

  private async loadUserProfile(): Promise<void> {
    try {
      const parsedToken = this.keycloakInstance.tokenParsed;
      const roles: string[] = parsedToken?.realm_access?.roles || [];
      
      let rolPrincipal = 'VENDEDOR';
      if (roles.includes('ADMIN')) rolPrincipal = 'ADMIN';
      else if (roles.includes('JEFE')) rolPrincipal = 'JEFE';

      this.userProfile = {
        id: 1, // Mapeado contra ID interno o sub
        nombre: parsedToken?.given_name || parsedToken?.name || 'Usuario',
        apellido: parsedToken?.family_name || '',
        correo: parsedToken?.email || parsedToken?.preferred_username || '',
        rol: rolPrincipal,
        estado: 'ACTIVO'
      };
    } catch (e) {
      console.error('[KeycloakAuthService] Error cargando perfil:', e);
    }
  }

  private setupTokenRefresh(): void {
    setInterval(async () => {
      try {
        if (this.keycloakInstance && this.isAuthSubject.value) {
          const refreshed = await this.keycloakInstance.updateToken(KEYCLOAK_CONFIG.minTokenValiditySeconds);
          if (refreshed) {
            console.debug('[KeycloakAuthService] Token de acceso renovado exitosamente.');
          }
        }
      } catch (error) {
        console.warn('[KeycloakAuthService] Error al renovar token, forzando re-login:', error);
        this.logout();
      }
    }, 20000);
  }
}
