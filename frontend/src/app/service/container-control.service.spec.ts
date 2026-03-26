import { TestBed } from '@angular/core/testing';
import { provideRouter, withNavigationErrorHandler } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import axios from 'axios';

import { ContainerControlService } from './container-control.service';
import { AuthService } from './auth.service';

vi.mock('axios', () => ({
  default: {
    post: vi.fn(),
    get: vi.fn(),
  },
}));

function createFakeJwt(payload: object): string {
  const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
  const body = btoa(JSON.stringify(payload));
  return `${header}.${body}.fakesignature`;
}

describe('ContainerControlService', () => {
  let service: ContainerControlService;

  beforeEach(() => {
    sessionStorage.clear();
    vi.clearAllMocks();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([], withNavigationErrorHandler(() => {})),
        ContainerControlService,
        AuthService,
      ],
    });
    service = TestBed.inject(ContainerControlService);
  });

  afterEach(() => {
    sessionStorage.clear();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should have correct baseURL', () => {
    expect(service.baseURL).toBe('http://localhost:9090/api/container');
  });

  // ── State management ────────────────────────────────────────────────────────

  describe('initial state', () => {
    it('should emit null selectedImage and isActive=false on subscribe', async () => {
      const state = await firstValueFrom(service.state$);
      expect(state.selectedImage).toBeNull();
      expect(state.isActive).toBe(false);
    });
  });

  describe('setSelectedImage', () => {
    it('should set selectedImage and isActive=true', async () => {
      const image = { ID: 1n, Name: 'nginx', URL: 'http://hub.docker.com/nginx' };
      service.setSelectedImage(image);
      const state = await firstValueFrom(service.state$);
      expect(state.selectedImage).toEqual(image);
      expect(state.isActive).toBe(true);
    });

    it('should set isActive=false when passing null', async () => {
      const image = { ID: 1n, Name: 'nginx', URL: 'http://hub.docker.com/nginx' };
      service.setSelectedImage(image);
      service.setSelectedImage(null);
      const state = await firstValueFrom(service.state$);
      expect(state.selectedImage).toBeNull();
      expect(state.isActive).toBe(false);
    });

    it('should emit a new state on each call', () => {
      const emitted: boolean[] = [];
      service.state$.subscribe((s) => emitted.push(s.isActive));
      service.setSelectedImage({ ID: 1n, Name: 'a', URL: '' });
      service.setSelectedImage(null);
      // BehaviorSubject always has at least the current value + 2 updates
      expect(emitted.length).toBeGreaterThanOrEqual(3);
    });
  });

  describe('clearSelectedImage', () => {
    it('should reset selectedImage to null and isActive to false', async () => {
      service.setSelectedImage({ ID: 2n, Name: 'alpine', URL: 'http://example.com' });
      service.clearSelectedImage();
      const state = await firstValueFrom(service.state$);
      expect(state.selectedImage).toBeNull();
      expect(state.isActive).toBe(false);
    });
  });

  // ── startContainer ──────────────────────────────────────────────────────────

  describe('startContainer', () => {
    it('should call axios.post with the start endpoint and correct body', async () => {
      vi.mocked(axios.post).mockResolvedValue({});
      await service.startContainer(1, 2);
      expect(axios.post).toHaveBeenCalledWith(
        'http://localhost:9090/api/container/start',
        { userId: 1, imageId: 2 },
        expect.any(Object),
      );
    });

    it('should not include Authorization header when no token is set', async () => {
      vi.mocked(axios.post).mockResolvedValue({});
      await service.startContainer(1, 2);
      const headers = (axios.post as ReturnType<typeof vi.fn>).mock.calls[0][2].headers;
      expect(headers).not.toHaveProperty('Authorization');
    });

    it('should include Authorization header when a valid token is stored', async () => {
      const token = createFakeJwt({ exp: Math.floor(Date.now() / 1000) + 3600 });
      sessionStorage.setItem('auth_token', token);
      TestBed.resetTestingModule();
      TestBed.configureTestingModule({
        providers: [
          provideRouter([], withNavigationErrorHandler(() => {})),
          AuthService,
          ContainerControlService,
        ],
      });
      const freshService = TestBed.inject(ContainerControlService);
      vi.mocked(axios.post).mockResolvedValue({});

      await freshService.startContainer(5, 10);

      const headers = (axios.post as ReturnType<typeof vi.fn>).mock.calls[0][2].headers;
      expect(headers.Authorization).toBe(`Bearer ${token}`);
    });

    it('should throw and propagate the error on axios failure', async () => {
      vi.mocked(axios.post).mockRejectedValue(new Error('Network error'));
      await expect(service.startContainer(1, 2)).rejects.toThrow('Network error');
    });
  });

  // ── stopContainer ───────────────────────────────────────────────────────────

  describe('stopContainer', () => {
    it('should call axios.post with the stop endpoint', async () => {
      vi.mocked(axios.post).mockResolvedValue({});
      await service.stopContainer(3, 4);
      expect(axios.post).toHaveBeenCalledWith(
        'http://localhost:9090/api/container/stop',
        { userId: 3, imageId: 4 },
        expect.any(Object),
      );
    });

    it('should throw on axios failure', async () => {
      vi.mocked(axios.post).mockRejectedValue(new Error('Stop failed'));
      await expect(service.stopContainer(1, 2)).rejects.toThrow('Stop failed');
    });
  });

  // ── restartContainer ────────────────────────────────────────────────────────

  describe('restartContainer', () => {
    it('should call axios.post with the reset endpoint', async () => {
      vi.mocked(axios.post).mockResolvedValue({});
      await service.restartContainer(7, 8);
      expect(axios.post).toHaveBeenCalledWith(
        'http://localhost:9090/api/container/reset',
        { userId: 7, imageId: 8 },
        expect.any(Object),
      );
    });

    it('should throw on axios failure', async () => {
      vi.mocked(axios.post).mockRejectedValue(new Error('Reset failed'));
      await expect(service.restartContainer(1, 2)).rejects.toThrow('Reset failed');
    });
  });
});
