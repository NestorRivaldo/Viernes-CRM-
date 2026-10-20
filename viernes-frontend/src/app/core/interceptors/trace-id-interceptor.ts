import { HttpInterceptorFn } from '@angular/common/http';

export const traceIdInterceptor: HttpInterceptorFn = (req, next) => {
  // Solo enviar X-Trace-ID al backend de Viernes, no a Keycloak OIDC
  if (req.url.includes('/protocol/openid-connect') || req.url.includes(':8180')) {
    return next(req);
  }
  const traceId = crypto.randomUUID();
  return next(req.clone({ headers: req.headers.set('X-Trace-ID', traceId) }));
};
