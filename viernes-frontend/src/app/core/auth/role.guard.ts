import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

export const roleGuard: CanActivateFn = (route) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const roles = route.data['roles'] as string[] | undefined;
  if (!roles || roles.length === 0) {
    return true;
  }

  if (authService.hasRole(...roles)) {
    return true;
  }

  router.navigate(['/dashboard']);
  return false;
};
