import { TestBed } from '@angular/core/testing';
import { provideRouter, withNavigationErrorHandler } from '@angular/router';

import { PermissionService } from './permission.service';
import { AuthService } from './auth.service';

function createFakeJwt(payload: object): string {
  const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
  const body = btoa(JSON.stringify(payload));
  return `${header}.${body}.fakesignature`;
}

function setupWithToken(payload: object) {
  const token = createFakeJwt({ exp: Math.floor(Date.now() / 1000) + 3600, ...payload });
  sessionStorage.setItem('auth_token', token);
  TestBed.configureTestingModule({
    providers: [
      provideRouter([], withNavigationErrorHandler(() => {})),
      AuthService,
      PermissionService,
    ],
  });
  return TestBed.inject(PermissionService);
}

describe('PermissionService', () => {
  beforeEach(() => {
    sessionStorage.clear();
  });

  afterEach(() => {
    sessionStorage.clear();
  });

  it('should be created', () => {
    TestBed.configureTestingModule({
      providers: [provideRouter([], withNavigationErrorHandler(() => {})), AuthService],
    });
    const service = TestBed.inject(PermissionService);
    expect(service).toBeTruthy();
  });

  // ── canRead ────────────────────────────────────────────────────────────────

  describe('canRead', () => {
    it('should be false when not logged in', () => {
      TestBed.configureTestingModule({
        providers: [provideRouter([], withNavigationErrorHandler(() => {})), AuthService],
      });
      const service = TestBed.inject(PermissionService);
      expect(service.canRead()).toBe(false);
    });

    it('should be true when logged in as SCHUELER', () => {
      const service = setupWithToken({ roles: ['SCHUELER'], userId: 'u1' });
      expect(service.canRead()).toBe(true);
    });

    it('should be true when logged in as LEHRER', () => {
      const service = setupWithToken({ roles: ['LEHRER'], userId: 'u2' });
      expect(service.canRead()).toBe(true);
    });

    it('should be true when logged in as ADMIN', () => {
      const service = setupWithToken({ roles: ['ADMIN'], userId: 'u3' });
      expect(service.canRead()).toBe(true);
    });
  });

  // ── canWrite ───────────────────────────────────────────────────────────────

  describe('canWrite', () => {
    it('should be false when not logged in', () => {
      TestBed.configureTestingModule({
        providers: [provideRouter([], withNavigationErrorHandler(() => {})), AuthService],
      });
      const service = TestBed.inject(PermissionService);
      expect(service.canWrite()).toBe(false);
    });

    it('should be false for SCHUELER', () => {
      const service = setupWithToken({ roles: ['SCHUELER'] });
      expect(service.canWrite()).toBe(false);
    });

    it('should be false for LEHRER', () => {
      const service = setupWithToken({ roles: ['LEHRER'] });
      expect(service.canWrite()).toBe(false);
    });

    it('should be true for ADMIN', () => {
      const service = setupWithToken({ roles: ['ADMIN'] });
      expect(service.canWrite()).toBe(true);
    });
  });

  // ── canGrade ───────────────────────────────────────────────────────────────

  describe('canGrade', () => {
    it('should be false when not logged in', () => {
      TestBed.configureTestingModule({
        providers: [provideRouter([], withNavigationErrorHandler(() => {})), AuthService],
      });
      const service = TestBed.inject(PermissionService);
      expect(service.canGrade()).toBe(false);
    });

    it('should be false for SCHUELER', () => {
      const service = setupWithToken({ roles: ['SCHUELER'] });
      expect(service.canGrade()).toBe(false);
    });

    it('should be true for LEHRER', () => {
      const service = setupWithToken({ roles: ['LEHRER'] });
      expect(service.canGrade()).toBe(true);
    });

    it('should be true for ADMIN', () => {
      const service = setupWithToken({ roles: ['ADMIN'] });
      expect(service.canGrade()).toBe(true);
    });
  });

  // ── canManageContainer ─────────────────────────────────────────────────────

  describe('canManageContainer', () => {
    it('should return true for ADMIN regardless of ownerId', () => {
      const service = setupWithToken({ roles: ['ADMIN'], userId: 99 });
      expect(service.canManageContainer('other-user')).toBe(true);
      expect(service.canManageContainer(null)).toBe(true);
      expect(service.canManageContainer(undefined)).toBe(true);
    });

    it('should return true for SCHUELER when ownerId matches userId (string)', () => {
      const service = setupWithToken({ roles: ['SCHUELER'], userId: 'student42' });
      expect(service.canManageContainer('student42')).toBe(true);
    });

    it('should return true for SCHUELER when ownerId matches userId (numeric)', () => {
      const service = setupWithToken({ roles: ['SCHUELER'], userId: 7 });
      expect(service.canManageContainer(7)).toBe(true);
    });

    it('should return true when ownerId is numeric string matching numeric userId', () => {
      const service = setupWithToken({ roles: ['SCHUELER'], userId: 7 });
      expect(service.canManageContainer('7')).toBe(true);
    });

    it('should return false for SCHUELER when ownerId does not match', () => {
      const service = setupWithToken({ roles: ['SCHUELER'], userId: 'alice' });
      expect(service.canManageContainer('bob')).toBe(false);
    });

    it('should return false when ownerId is null', () => {
      const service = setupWithToken({ roles: ['SCHUELER'], userId: 'alice' });
      expect(service.canManageContainer(null)).toBe(false);
    });

    it('should return false when ownerId is undefined', () => {
      const service = setupWithToken({ roles: ['SCHUELER'], userId: 'alice' });
      expect(service.canManageContainer(undefined)).toBe(false);
    });

    it('should return false when not logged in', () => {
      TestBed.configureTestingModule({
        providers: [provideRouter([], withNavigationErrorHandler(() => {})), AuthService],
      });
      const service = TestBed.inject(PermissionService);
      expect(service.canManageContainer('anyone')).toBe(false);
    });
  });
});
