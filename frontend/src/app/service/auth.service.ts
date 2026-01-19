import { Injectable, signal } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private isAuthenticated = signal<boolean>(false);

  isLoggedIn() {
    return this.isAuthenticated.asReadonly();
  }

  login(email: string, password: string): boolean {
    if (email === 'test@gmail.com' && password === 'test') {
      this.isAuthenticated.set(true);
      return true;
    }
    return false;
  }

  logout() {
    this.isAuthenticated.set(false);
  }
}
