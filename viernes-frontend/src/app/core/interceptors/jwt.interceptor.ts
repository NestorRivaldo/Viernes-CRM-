import {
  HttpErrorResponse,
  HttpEvent,
  HttpHandlerFn,
  HttpInterceptorFn,
  HttpRequest
} from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import {
  Observable,
  catchError,
  from,
  mergeMap,
  switchMap,
  throwError,
  timeout,
  TimeoutError
} from 'rxjs';
import { AuthService } from '../auth/auth.service';

const REQUEST_TIMEOUT_MS = 15000;

export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const token = authService.getToken();

  const isAuthRequest = req.url.includes('/protocol/openid-connect') || req.url.includes('/api/v1/auth/');

  const authReq = token && !isAuthRequest
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(authReq).pipe(
    timeout(REQUEST_TIMEOUT_MS),
    catchError((error: HttpErrorResponse) => {
      if (error instanceof TimeoutError) {
        (error as any).friendlyMessage = 'El servidor no responde. Verifique que el backend esté disponible.';
        return throwError(() => error);
      }
      if (error.status === 401 && !isAuthRequest) {
        return refreshAndRetry(req, next, authService, router, error);
      }
      if (error.status === 403) {
        alert('No tiene permisos para realizar esta acción.');
      }
      (error as any).friendlyMessage = buildFriendlyMessage(error);
      return throwError(() => error);
    })
  );
};

/**
 * Ante un 401 intenta renovar el token una sola vez y reintenta la petición.
 * Si no hay token nuevo, o el reintento vuelve a fallar con 401, cierra la
 * sesión contra Keycloak (RP-Initiated Logout).
 */
function refreshAndRetry(
  req: HttpRequest<unknown>,
  next: HttpHandlerFn,
  authService: AuthService,
  router: Router,
  originalError: HttpErrorResponse
): Observable<HttpEvent<unknown>> {
  return from(authService.getTokenAsync()).pipe(
    switchMap((newToken) => {
      if (!newToken) {
        return closeSession(authService, router, originalError);
      }

      const retryReq = req.clone({
        setHeaders: { Authorization: `Bearer ${newToken}` }
      });

      return next(retryReq).pipe(
        timeout(REQUEST_TIMEOUT_MS),
        catchError((retryError: HttpErrorResponse) => {
          if (retryError instanceof TimeoutError) {
            (retryError as any).friendlyMessage = 'El servidor no responde. Verifique que el backend esté disponible.';
            return throwError(() => retryError);
          }
          if (retryError.status === 401) {
            return closeSession(authService, router, retryError);
          }
          if (retryError.status === 403) {
            alert('No tiene permisos para realizar esta acción.');
          }
          (retryError as any).friendlyMessage = buildFriendlyMessage(retryError);
          return throwError(() => retryError);
        })
      );
    })
  );
}

function closeSession(
  authService: AuthService,
  router: Router,
  error: HttpErrorResponse
): Observable<never> {
  (error as any).friendlyMessage = buildFriendlyMessage(error);
  return from(
    authService.logout().catch(() => {
      void router.navigate(['/login']);
    })
  ).pipe(mergeMap(() => throwError(() => error)));
}

function buildFriendlyMessage(error: HttpErrorResponse): string {
  if (error.status === 0) {
    return 'No se pudo conectar con el servidor. Verifique que el backend y Keycloak estén disponibles.';
  }
  const body: any = error.error;
  if (body?.fieldErrors) {
    const detalles = Object.values(body.fieldErrors).join('. ');
    return `Error de validación: ${detalles}`;
  }
  if (typeof body?.message === 'string' && body.message) {
    return body.message;
  }
  if (typeof body?.error_description === 'string' && body.error_description) {
    return body.error_description;
  }
  if (error.status === 401) {
    return 'Su sesión expiró. Inicie sesión nuevamente.';
  }
  if (error.status === 403) {
    return 'No tiene permisos para realizar esta acción.';
  }
  if (error.status === 404) {
    return 'El recurso solicitado no existe.';
  }
  if (error.status >= 500) {
    return 'Error interno del servidor. Intente nuevamente más tarde.';
  }
  return `Error HTTP ${error.status}`;
}
