import { inject } from '@angular/core';
import { CanActivateFn } from '@angular/router';
import { AuthService } from '../service/auth.service';

export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);

  if (authService.isLoggedIn()() && !authService.isTokenExpired()) {
    return true;
  }

  // Token abgelaufen oder nicht eingeloggt → logout + Login-Seite
  authService.logout();
  return false;
};

