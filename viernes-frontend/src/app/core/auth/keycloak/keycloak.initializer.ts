import { EnvironmentProviders, inject, provideAppInitializer } from '@angular/core';
import { KeycloakAuthService } from './keycloak-auth.service';

/**
 * Inicializa Keycloak antes de que Angular monte las rutas.
 * El callback de authorization code se procesa aquí, de modo que
 * los guards ya encuentran la sesión lista.
 */
export function provideKeycloak(): EnvironmentProviders {
  return provideAppInitializer(() =>
    inject(KeycloakAuthService)
      .init()
      .then(() => undefined)
  );
}
