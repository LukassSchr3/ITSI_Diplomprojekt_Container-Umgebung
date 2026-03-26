import { TestBed } from '@angular/core/testing';

import { ThemeService } from './theme.service';

function mockMatchMedia(prefersDark: boolean) {
  Object.defineProperty(window, 'matchMedia', {
    writable: true,
    value: vi.fn((query: string) => ({
      matches: query.includes('dark') ? prefersDark : false,
      media: query,
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      dispatchEvent: vi.fn(),
    })),
  });
}

describe('ThemeService', () => {
  beforeEach(() => {
    localStorage.clear();
    mockMatchMedia(false);
  });

  afterEach(() => {
    localStorage.clear();
  });

  function createService(): ThemeService {
    TestBed.configureTestingModule({});
    return TestBed.inject(ThemeService);
  }

  it('should be created', () => {
    expect(createService()).toBeTruthy();
  });

  // ── Initial theme resolution ───────────────────────────────────────────────

  describe('initial theme', () => {
    it('should default to "light" when no saved theme and no dark-mode preference', () => {
      mockMatchMedia(false);
      const service = createService();
      expect(service.theme()).toBe('light');
    });

    it('should default to "dark" when no saved theme but system prefers dark', () => {
      mockMatchMedia(true);
      const service = createService();
      expect(service.theme()).toBe('dark');
    });

    it('should restore "light" from localStorage', () => {
      localStorage.setItem('app-theme', 'light');
      const service = createService();
      expect(service.theme()).toBe('light');
    });

    it('should restore "dark" from localStorage', () => {
      localStorage.setItem('app-theme', 'dark');
      const service = createService();
      expect(service.theme()).toBe('dark');
    });

    it('should prefer localStorage over system preference', () => {
      localStorage.setItem('app-theme', 'light');
      mockMatchMedia(true); // system wants dark – localStorage wins
      const service = createService();
      expect(service.theme()).toBe('light');
    });
  });

  // ── toggleTheme ────────────────────────────────────────────────────────────

  describe('toggleTheme', () => {
    it('should switch from light to dark', () => {
      const service = createService();
      service.setTheme('light');
      service.toggleTheme();
      expect(service.theme()).toBe('dark');
    });

    it('should switch from dark to light', () => {
      const service = createService();
      service.setTheme('dark');
      service.toggleTheme();
      expect(service.theme()).toBe('light');
    });

    it('should toggle back and forth correctly', () => {
      const service = createService();
      service.setTheme('light');
      service.toggleTheme();
      service.toggleTheme();
      expect(service.theme()).toBe('light');
    });
  });

  // ── setTheme ───────────────────────────────────────────────────────────────

  describe('setTheme', () => {
    it('should set theme to "dark"', () => {
      const service = createService();
      service.setTheme('dark');
      expect(service.theme()).toBe('dark');
    });

    it('should set theme to "light"', () => {
      const service = createService();
      service.setTheme('dark');
      service.setTheme('light');
      expect(service.theme()).toBe('light');
    });
  });

});

