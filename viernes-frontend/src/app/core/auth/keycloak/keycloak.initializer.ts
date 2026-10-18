import { Provider, APP_INITIALIZER } from '@angular/core';
import { KeycloakAuthService } from './keycloak-auth.service';

/**
 * Factory para inicializar Keycloak antes de cargar cualquier ruta de la aplicación.
 */
export function initializeKeycloak(keycloakAuthService: KeycloakAuthService) {
  return () => keycloakAuthService.init();
}

/**
 * Proveedor para registrar en app.config.ts cuando se active Keycloak:
 * 
 * export const appConfig: ApplicationConfig = {
 *   providers: [
 *     provideKeycloak(),
 *     ...
 *   ]
 * };
 */
export function provideKeycloak(): Provider {
  return {
    provide: APP_INITIALIZER,
    useFactory: initializeKeycloak,
    multi: true,
    deps: [KeycloakAuthService]
  };
}
