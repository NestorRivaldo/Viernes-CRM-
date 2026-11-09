import {
  ApplicationConfig,
  provideBrowserGlobalErrorListeners,
  provideZonelessChangeDetection,
} from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { routes } from './app.routes';
import { traceIdInterceptor } from './core/interceptors/trace-id-interceptor';
import { jwtInterceptor } from './core/interceptors/jwt.interceptor';
import { provideKeycloak } from './core/auth/keycloak/keycloak.initializer';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZonelessChangeDetection(),
    provideKeycloak(),
    provideRouter(routes),
    provideHttpClient(withInterceptors([traceIdInterceptor, jwtInterceptor])),
  ],
};
