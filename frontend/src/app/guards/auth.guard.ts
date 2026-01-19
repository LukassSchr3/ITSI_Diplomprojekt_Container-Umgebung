import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '../service/auth.service';

export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isLoggedIn()()) {
    return true;
  }

  // Nicht eingeloggt - zum Login umleiten
  router.navigate(['/login']);
  return false;
};

