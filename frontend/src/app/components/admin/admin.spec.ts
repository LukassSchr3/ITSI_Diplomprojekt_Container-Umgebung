import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { Admin } from './admin';

describe('Admin', () => {
  let component: Admin;
  let fixture: ComponentFixture<Admin>;

  beforeEach(async () => {
    // Mock localStorage for ThemeService
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

    // Mock window.matchMedia for ThemeService
    Object.defineProperty(window, 'matchMedia', {
      writable: true,
      configurable: true,
      value: (query: string) => ({
        matches: false,
        media: query,
        onchange: null,
        addListener: () => true,
        removeListener: () => true,
        addEventListener: () => true,
        removeEventListener: () => true,
        dispatchEvent: () => false,
      }),
    });

    await TestBed.configureTestingModule({
      imports: [Admin],
      providers: [provideRouter([])],
    })
    .compileComponents();

    fixture = TestBed.createComponent(Admin);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
