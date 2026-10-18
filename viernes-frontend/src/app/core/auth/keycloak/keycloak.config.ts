/**
 * Configuración de conexión con el servidor Keycloak para Viernes V1.
 * 
 * En desarrollo local:
 * - Puerto de Keycloak: 8180 (para no colisionar con Spring Boot en 8080)
 * - Realm: viernes-realm
 * - Client ID público: viernes-frontend
 */
export const KEYCLOAK_CONFIG = {
  url: 'http://localhost:8180',
  realm: 'viernes-realm',
  clientId: 'viernes-frontend',
  minTokenValiditySeconds: 30, // Renovación automática si expira en < 30s
  initOptions: {
    onLoad: 'check-sso' as const, // 'check-sso' no fuerza login si no tiene sesión; 'login-required' fuerza login directo
    checkLoginIframe: false,
    pkceMethod: 'S256' as const,
    enableLogging: true
  }
};
