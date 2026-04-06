import { TestBed } from '@angular/core/testing';
import { vi, beforeEach, afterEach, describe, it, expect } from 'vitest';

import { ThemeService } from './theme.service';

function mockMatchMedia(prefersDark: boolean) {
  Object.defineProperty(window, 'matchMedia', {
    writable: true,
    configurable: true,
    value: vi.fn((query: string) => ({
      matches: query.includes('dark') ? prefersDark : false,
      media: query,
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      dispatchEvent: vi.fn(),
      addListener: vi.fn(),
      removeListener: vi.fn(),
    })),
  });
}

function mockLocalStorage() {
  let store: Record<string, string> = {};

  Object.defineProperty(window, 'localStorage', {
    value: {
      getItem: (key: string) => store[key] || null,
      setItem: (key: string, value: string) => {
        store[key] = value;
      },
      removeItem: (key: string) => {
        delete store[key];
      },
      clear: () => {
        store = {};
      },
      key: (index: number) => Object.keys(store)[index] || null,
      length: Object.keys(store).length,
    },
    writable: true,
    configurable: true,
  });
}

describe('ThemeService', () => {
  beforeEach(() => {
    mockLocalStorage();
    localStorage.clear();
    mockMatchMedia(false);
    TestBed.resetTestingModule();
  });

  afterEach(() => {
    localStorage.clear();
  });

  function createService(): ThemeService {
    TestBed.configureTestingModule({
      providers: [ThemeService],
    });
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

