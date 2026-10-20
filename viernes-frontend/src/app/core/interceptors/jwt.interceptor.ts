import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError, timeout, TimeoutError } from 'rxjs';
import { AuthService } from '../auth/auth.service';

export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const token = authService.getToken();

  const isAuthRequest = req.url.includes('/protocol/openid-connect') || req.url.includes('/api/v1/auth/');

  if (token && !isAuthRequest) {
    req = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }

  return next(req).pipe(
    timeout(15000),
    catchError((error: HttpErrorResponse) => {
      if (error instanceof TimeoutError) {
        (error as any).friendlyMessage = 'El servidor no responde. Verifique que el backend esté disponible.';
        return throwError(() => error);
      }
      if (error.status === 401 && !isAuthRequest) {
        authService.logout();
        router.navigate(['/login']);
      } else if (error.status === 403) {
        alert('No tiene permisos para realizar esta acción.');
      }
      (error as any).friendlyMessage = buildFriendlyMessage(error);
      return throwError(() => error);
    })
  );
};

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
