/**
 * Configuración de conexión con Keycloak para Viernes V1.
 *
 * Flujo: Authorization Code + PKCE (S256).
 * Keycloak no recibe contraseñas desde la SPA: el usuario autentica
 * directamente en el portal de identidad.
 */
export const KEYCLOAK_CONFIG = {
  url: 'http://localhost:8180',
  realm: 'viernes-realm',
  clientId: 'viernes-frontend',
  minTokenValiditySeconds: 30,
  initOptions: {
    onLoad: 'check-sso' as const,
    checkLoginIframe: false,
    pkceMethod: 'S256' as const,
    flow: 'standard' as const,
    scope: 'openid profile email',
    enableLogging: false
  }
};
