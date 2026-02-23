import { Injectable, computed, signal, inject } from '@angular/core';
import { Router } from '@angular/router';
import axios from 'axios';

export type UserRole = 'SCHUELER' | 'LEHRER' | 'ADMIN';

interface LoginResponse {
  token: string;
}

interface JwtClaims {
  sub?: string;
  email?: string;
  userId?: string | number;
  roles?: UserRole[];
  role?: UserRole;
  rolle?: UserRole;
  exp?: number;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private router = inject(Router);
  private token = signal<string | null>(null);
  private userId = signal<string | number | null>(null);
  private roles = signal<UserRole[]>([]);

  constructor() {
    const stored = sessionStorage.getItem('auth_token');
    if (stored) {
      if (this.isTokenExpired(stored)) {
        sessionStorage.removeItem('auth_token');
      } else {
        this.setToken(stored);
      }
    }

    // Alle 30 Sekunden prüfen ob Token abgelaufen ist
    setInterval(() => {
      const token = this.token();
      if (token && this.isTokenExpired(token)) {
        this.logout();
      }
    }, 30_000);
  }

  isLoggedIn() {
    return computed(() => !!this.token());
  }

  getToken() {
    return this.token.asReadonly();
  }

  getUserId() {
    return this.userId.asReadonly();
  }

  getRoles() {
    return this.roles.asReadonly();
  }

  isAdmin() {
    return computed(() => this.roles().includes('ADMIN'));
  }

  isTeacher() {
    return computed(() => this.roles().includes('LEHRER'));
  }

  isStudent() {
    return computed(() => this.roles().includes('SCHUELER'));
  }

  async login(email: string, password: string): Promise<boolean> {
    try {
      const response = await axios.post<LoginResponse>('http://localhost:9090/api/auth/login', {
        email,
        password
      });

      if (!response.data?.token) {
        return false;
      }

      this.setToken(response.data.token);
      console.log(this.getToken()());
      console.log(this.getRoles()());
      console.log(this.isAdmin()());
      return true;
    } catch {
      return false;
    }
  }

  logout() {
    this.token.set(null);
    this.userId.set(null);
    this.roles.set([]);
    sessionStorage.removeItem('auth_token');
    this.router.navigate(['/login']);
  }

  private setToken(token: string) {
    this.token.set(token);
    sessionStorage.setItem('auth_token', token);

    const claims = this.parseJwt(token);
    const userId = claims?.userId ?? claims?.sub ?? claims?.email ?? null;
    this.userId.set(userId);

    const roles = claims?.roles ?? (claims?.role ? [claims.role] : claims?.rolle ? [claims.rolle] : []);
    this.roles.set(roles);
  }

  isTokenExpired(token?: string): boolean {
    const t = token ?? this.token();
    if (!t) return true;
    const claims = this.parseJwt(t);
    if (!claims?.exp) return false; // kein exp-Claim → nicht als abgelaufen werten
    return claims.exp * 1000 < Date.now();
  }

  private parseJwt(token: string): JwtClaims | null {
    try {
      const payload = token.split('.')[1];
      const normalized = payload.replace(/-/g, '+').replace(/_/g, '/');
      const padded = normalized.padEnd(normalized.length + (4 - normalized.length % 4) % 4, '=');
      return JSON.parse(atob(padded)) as JwtClaims;
    } catch {
      return null;
    }
  }
}
