import { TestBed } from '@angular/core/testing';
import { provideRouter, withNavigationErrorHandler } from '@angular/router';
import axios from 'axios';

import { AuthService } from './auth.service';

vi.mock('axios', () => ({
  default: {
    post: vi.fn(),
  },
}));

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
      providers: [provideRouter([], withNavigationErrorHandler(() => {}))],
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

    it('should return false for a malformed token (treated as no exp claim)', () => {
      // parseJwt() catches errors and returns null; null (no exp) means not-expired by design
      expect(service.isTokenExpired('not-a-valid-jwt')).toBe(false);
    });
  });

  describe('logout', () => {
    it('should clear the token from sessionStorage on logout', () => {
      const validToken = createFakeJwt({ exp: Math.floor(Date.now() / 1000) + 3600, roles: ['SCHUELER'] });
      sessionStorage.setItem('auth_token', validToken);

      // Recreate service so it picks up the stored token
      TestBed.resetTestingModule();
      TestBed.configureTestingModule({ providers: [provideRouter([], withNavigationErrorHandler(() => {}))] });
      const freshService = TestBed.inject(AuthService);

      expect(freshService.isLoggedIn()()).toBe(true);

      freshService.logout();

      expect(sessionStorage.getItem('auth_token')).toBeNull();
    });

    it('should set isLoggedIn to false after logout', () => {
      const validToken = createFakeJwt({ exp: Math.floor(Date.now() / 1000) + 3600, roles: ['SCHUELER'] });
      sessionStorage.setItem('auth_token', validToken);
      TestBed.resetTestingModule();
      TestBed.configureTestingModule({ providers: [provideRouter([], withNavigationErrorHandler(() => {}))] });
      const freshService = TestBed.inject(AuthService);
      freshService.logout();
      expect(freshService.isLoggedIn()()).toBe(false);
    });
  });

  // ── Token restoration from sessionStorage ───────────────────────────────────

  describe('token restoration', () => {
    it('should restore a valid token from sessionStorage on construction', () => {
      const token = createFakeJwt({ exp: Math.floor(Date.now() / 1000) + 3600, roles: ['SCHUELER'] });
      sessionStorage.setItem('auth_token', token);
      TestBed.resetTestingModule();
      TestBed.configureTestingModule({ providers: [provideRouter([], withNavigationErrorHandler(() => {}))] });
      const freshService = TestBed.inject(AuthService);
      expect(freshService.isLoggedIn()()).toBe(true);
      expect(freshService.getToken()()).toBe(token);
    });

    it('should discard an expired token from sessionStorage on construction', () => {
      const expired = createFakeJwt({ exp: Math.floor(Date.now() / 1000) - 60, roles: ['SCHUELER'] });
      sessionStorage.setItem('auth_token', expired);
      TestBed.resetTestingModule();
      TestBed.configureTestingModule({ providers: [provideRouter([], withNavigationErrorHandler(() => {}))] });
      const freshService = TestBed.inject(AuthService);
      expect(freshService.isLoggedIn()()).toBe(false);
      expect(sessionStorage.getItem('auth_token')).toBeNull();
    });
  });

  // ── Role parsing ────────────────────────────────────────────────────────────

  describe('role parsing from JWT', () => {
    function buildAndLogin(payload: object): AuthService {
      const token = createFakeJwt({ exp: Math.floor(Date.now() / 1000) + 3600, ...payload });
      sessionStorage.setItem('auth_token', token);
      TestBed.resetTestingModule();
      TestBed.configureTestingModule({ providers: [provideRouter([], withNavigationErrorHandler(() => {}))] });
      return TestBed.inject(AuthService);
    }

    it('should parse roles from the "roles" array claim', () => {
      const svc = buildAndLogin({ roles: ['ADMIN', 'LEHRER'] });
      expect(svc.getRoles()()).toContain('ADMIN');
      expect(svc.getRoles()()).toContain('LEHRER');
    });

    it('should parse a single role from the "role" string claim', () => {
      const svc = buildAndLogin({ role: 'LEHRER' });
      expect(svc.getRoles()()).toEqual(['LEHRER']);
      expect(svc.isTeacher()()).toBe(true);
    });

    it('should parse a single role from the "rolle" string claim', () => {
      const svc = buildAndLogin({ rolle: 'SCHUELER' });
      expect(svc.getRoles()()).toEqual(['SCHUELER']);
      expect(svc.isStudent()()).toBe(true);
    });

    it('should set isAdmin() true when roles contains ADMIN', () => {
      const svc = buildAndLogin({ roles: ['ADMIN'] });
      expect(svc.isAdmin()()).toBe(true);
    });

    it('should set isTeacher() true when roles contains LEHRER', () => {
      const svc = buildAndLogin({ roles: ['LEHRER'] });
      expect(svc.isTeacher()()).toBe(true);
    });

    it('should set isStudent() true when roles contains SCHUELER', () => {
      const svc = buildAndLogin({ roles: ['SCHUELER'] });
      expect(svc.isStudent()()).toBe(true);
    });
  });

  // ── userId parsing ──────────────────────────────────────────────────────────

  describe('userId parsing from JWT', () => {
    function buildAndLogin(payload: object): AuthService {
      const token = createFakeJwt({ exp: Math.floor(Date.now() / 1000) + 3600, ...payload });
      sessionStorage.setItem('auth_token', token);
      TestBed.resetTestingModule();
      TestBed.configureTestingModule({ providers: [provideRouter([], withNavigationErrorHandler(() => {}))] });
      return TestBed.inject(AuthService);
    }

    it('should extract userId from the "userId" claim', () => {
      const svc = buildAndLogin({ userId: 42 });
      expect(svc.getUserId()()).toBe(42);
    });

    it('should fall back to "sub" claim when "userId" is absent', () => {
      const svc = buildAndLogin({ sub: 'user@tgm.ac.at' });
      expect(svc.getUserId()()).toBe('user@tgm.ac.at');
    });

    it('should fall back to "email" claim when both "userId" and "sub" are absent', () => {
      const svc = buildAndLogin({ email: 'student@tgm.ac.at' });
      expect(svc.getUserId()()).toBe('student@tgm.ac.at');
    });

    it('should be null when no identifying claim is present', () => {
      const svc = buildAndLogin({ roles: ['SCHUELER'] });
      expect(svc.getUserId()()).toBeNull();
    });
  });

  // ── login() via axios ────────────────────────────────────────────────────────

  describe('login()', () => {
    beforeEach(() => {
      vi.clearAllMocks();
    });

    it('should return true and set token on successful login', async () => {
      const token = createFakeJwt({ exp: Math.floor(Date.now() / 1000) + 3600, roles: ['SCHUELER'], userId: 1 });
      vi.mocked(axios.post).mockResolvedValue({ data: { token } });

      const result = await service.login('student@example.com', 'password123');

      expect(result).toBe(true);
      expect(service.isLoggedIn()()).toBe(true);
      expect(service.getToken()()).toBe(token);
    });

    it('should return false when the response has no token', async () => {
      vi.mocked(axios.post).mockResolvedValue({ data: {} });
      const result = await service.login('user@example.com', 'pass');
      expect(result).toBe(false);
      expect(service.isLoggedIn()()).toBe(false);
    });

    it('should return false when axios throws', async () => {
      vi.mocked(axios.post).mockRejectedValue(new Error('Connection refused'));
      const result = await service.login('user@example.com', 'pass');
      expect(result).toBe(false);
      expect(service.isLoggedIn()()).toBe(false);
    });

    it('should call the login endpoint with correct email and password', async () => {
      vi.mocked(axios.post).mockResolvedValue({ data: {} });
      await service.login('admin@tgm.ac.at', 'supersecret');
      expect(axios.post).toHaveBeenCalledWith(
        'http://localhost:9090/api/auth/login',
        { email: 'admin@tgm.ac.at', password: 'supersecret' },
      );
    });

    it('should persist token to sessionStorage on successful login', async () => {
      const token = createFakeJwt({ exp: Math.floor(Date.now() / 1000) + 3600, roles: ['ADMIN'] });
      vi.mocked(axios.post).mockResolvedValue({ data: { token } });
      await service.login('admin@example.com', 'pass');
      expect(sessionStorage.getItem('auth_token')).toBe(token);
    });
  });
});

