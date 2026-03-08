import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { AuthService } from './auth.service';

/** Creates a fake JWT token with the given payload (not cryptographically valid, but parsable). */
function createFakeJwt(payload: object): string {
  const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
  const body = btoa(JSON.stringify(payload));
  return `${header}.${body}.fakesignature`;
}

describe('AuthService', () => {
  let service: AuthService;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideRouter([])],
    });
    service = TestBed.inject(AuthService);
  });

  afterEach(() => {
    sessionStorage.clear();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should not be logged in initially', () => {
    expect(service.isLoggedIn()()).toBe(false);
  });

  it('should have empty roles initially', () => {
    expect(service.getRoles()()).toEqual([]);
  });

  it('should have no userId initially', () => {
    expect(service.getUserId()()).toBeNull();
  });

  it('should not be admin initially', () => {
    expect(service.isAdmin()()).toBe(false);
  });

  it('should not be teacher initially', () => {
    expect(service.isTeacher()()).toBe(false);
  });

  it('should not be student initially', () => {
    expect(service.isStudent()()).toBe(false);
  });

  describe('isTokenExpired', () => {
    it('should return true when no token is provided and not logged in', () => {
      expect(service.isTokenExpired()).toBe(true);
    });

    it('should return true for an expired token', () => {
      const expiredToken = createFakeJwt({ exp: Math.floor(Date.now() / 1000) - 3600 });
      expect(service.isTokenExpired(expiredToken)).toBe(true);
    });

    it('should return false for a valid token', () => {
      const validToken = createFakeJwt({ exp: Math.floor(Date.now() / 1000) + 3600 });
      expect(service.isTokenExpired(validToken)).toBe(false);
    });

    it('should return false for a token without an exp claim', () => {
      const noExpToken = createFakeJwt({ sub: 'user@example.com' });
      expect(service.isTokenExpired(noExpToken)).toBe(false);
    });

    it('should return true for a malformed token', () => {
      expect(service.isTokenExpired('not-a-valid-jwt')).toBe(true);
    });
  });

  describe('logout', () => {
    it('should clear the token from sessionStorage on logout', () => {
      const validToken = createFakeJwt({ exp: Math.floor(Date.now() / 1000) + 3600, roles: ['SCHUELER'] });
      sessionStorage.setItem('auth_token', validToken);

      // Recreate service so it picks up the stored token
      TestBed.resetTestingModule();
      TestBed.configureTestingModule({ providers: [provideRouter([])] });
      const freshService = TestBed.inject(AuthService);

      expect(freshService.isLoggedIn()()).toBe(true);

      freshService.logout();

      expect(sessionStorage.getItem('auth_token')).toBeNull();
    });
  });
});
