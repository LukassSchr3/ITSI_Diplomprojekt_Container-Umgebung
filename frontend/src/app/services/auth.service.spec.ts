import { TestBed } from '@angular/core/testing';

import { AuthService } from './auth.service';

describe('AuthService (services/)', () => {
  let service: AuthService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(AuthService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  // ── Initial state ──────────────────────────────────────────────────────────

  describe('initial state', () => {
    it('should not be logged in initially', () => {
      expect(service.isLoggedIn()()).toBe(false);
    });

    it('should have null role initially', () => {
      expect(service.getRole()()).toBeNull();
    });

    it('should report isTeacher() = false initially', () => {
      expect(service.isTeacher()()).toBe(false);
    });

    it('should report isStudent() = false initially', () => {
      expect(service.isStudent()()).toBe(false);
    });
  });

  // ── login – teacher ────────────────────────────────────────────────────────

  describe('login as teacher', () => {
    it('should return true with correct teacher credentials', () => {
      expect(service.login('lehrer@gmail.com', 'test')).toBe(true);
    });

    it('should set isLoggedIn to true after teacher login', () => {
      service.login('lehrer@gmail.com', 'test');
      expect(service.isLoggedIn()()).toBe(true);
    });

    it('should set role to "teacher" after teacher login', () => {
      service.login('lehrer@gmail.com', 'test');
      expect(service.getRole()()).toBe('teacher');
    });

    it('should report isTeacher() = true after teacher login', () => {
      service.login('lehrer@gmail.com', 'test');
      expect(service.isTeacher()()).toBe(true);
    });

    it('should report isStudent() = false after teacher login', () => {
      service.login('lehrer@gmail.com', 'test');
      expect(service.isStudent()()).toBe(false);
    });
  });

  // ── login – student ────────────────────────────────────────────────────────

  describe('login as student', () => {
    it('should return true with correct student credentials', () => {
      expect(service.login('schüler@gmail.com', 'test')).toBe(true);
    });

    it('should set isLoggedIn to true after student login', () => {
      service.login('schüler@gmail.com', 'test');
      expect(service.isLoggedIn()()).toBe(true);
    });

    it('should set role to "student" after student login', () => {
      service.login('schüler@gmail.com', 'test');
      expect(service.getRole()()).toBe('student');
    });

    it('should report isStudent() = true after student login', () => {
      service.login('schüler@gmail.com', 'test');
      expect(service.isStudent()()).toBe(true);
    });

    it('should report isTeacher() = false after student login', () => {
      service.login('schüler@gmail.com', 'test');
      expect(service.isTeacher()()).toBe(false);
    });
  });

  // ── login failures ─────────────────────────────────────────────────────────

  describe('login failures', () => {
    it('should return false with wrong password', () => {
      expect(service.login('lehrer@gmail.com', 'wrong')).toBe(false);
    });

    it('should not change login state on wrong password', () => {
      service.login('lehrer@gmail.com', 'wrong');
      expect(service.isLoggedIn()()).toBe(false);
    });

    it('should return false with unknown email (correct password pattern)', () => {
      expect(service.login('unknown@example.com', 'test')).toBe(false);
    });

    it('should not change login state on unknown email', () => {
      service.login('unknown@example.com', 'test');
      expect(service.isLoggedIn()()).toBe(false);
      expect(service.getRole()()).toBeNull();
    });

    it('should return false with empty email', () => {
      expect(service.login('', 'test')).toBe(false);
    });

    it('should return false with empty password', () => {
      expect(service.login('lehrer@gmail.com', '')).toBe(false);
    });

    it('should return false with both fields empty', () => {
      expect(service.login('', '')).toBe(false);
    });
  });

  // ── logout ─────────────────────────────────────────────────────────────────

  describe('logout', () => {
    it('should set isLoggedIn to false after logout', () => {
      service.login('lehrer@gmail.com', 'test');
      service.logout();
      expect(service.isLoggedIn()()).toBe(false);
    });

    it('should reset role to null after logout', () => {
      service.login('lehrer@gmail.com', 'test');
      service.logout();
      expect(service.getRole()()).toBeNull();
    });

    it('should reset isTeacher() to false after logout', () => {
      service.login('lehrer@gmail.com', 'test');
      service.logout();
      expect(service.isTeacher()()).toBe(false);
    });

    it('should reset isStudent() to false after logout', () => {
      service.login('schüler@gmail.com', 'test');
      service.logout();
      expect(service.isStudent()()).toBe(false);
    });

    it('should be safe to call logout when already logged out', () => {
      expect(() => service.logout()).not.toThrow();
      expect(service.isLoggedIn()()).toBe(false);
    });
  });

  // ── login then logout ──────────────────────────────────────────────────────

  describe('login-logout cycle', () => {
    it('should allow re-login as a different role after logout', () => {
      service.login('lehrer@gmail.com', 'test');
      service.logout();
      service.login('schüler@gmail.com', 'test');
      expect(service.getRole()()).toBe('student');
    });
  });
});
