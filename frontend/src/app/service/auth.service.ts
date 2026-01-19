import { Injectable, computed, signal } from '@angular/core';

type UserRole = 'teacher' | 'student';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private isAuthenticated = signal<boolean>(false);
  private role = signal<UserRole | null>(null);

  isLoggedIn() {
    return this.isAuthenticated.asReadonly();
  }

  getRole() {
    return this.role.asReadonly();
  }

  isTeacher() {
    return computed(() => this.role() === 'teacher');
  }

  isStudent() {
    return computed(() => this.role() === 'student');
  }

  login(email: string, password: string): boolean {
    // simples Frontend-Login ohne Backend
    if (password !== 'test') {
      return false;
    }

    if (email === 'lehrer@gmail.com') {
      this.isAuthenticated.set(true);
      this.role.set('teacher');
      return true;
    }

    if (email === 'schüler@gmail.com') {
      this.isAuthenticated.set(true);
      this.role.set('student');
      return true;
    }

    return false;
  }

  logout() {
    this.isAuthenticated.set(false);
    this.role.set(null);
  }
}
