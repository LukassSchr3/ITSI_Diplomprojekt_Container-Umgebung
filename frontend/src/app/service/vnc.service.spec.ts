import { TestBed } from '@angular/core/testing';
import { firstValueFrom, skip } from 'rxjs';

import { VncService, VNCConnectionStatus } from './vnc.service';

// ── Mock RFB class ───────────────────────────────────────────────────────────

class MockRFB {
  scaleViewport = false;
  resizeSession = false;

  private listeners: Record<string, (e?: any) => void> = {};

  constructor(
    public readonly target: HTMLElement,
    public readonly url: string,
  ) {}

  addEventListener(event: string, cb: (e?: any) => void) {
    this.listeners[event] = cb;
  }

  disconnect() {}
  sendCtrlAltDel() {}
  sendKey(_keysym: number, _down: boolean) {}

  /** Test helper – trigger a named event on this RFB mock. */
  trigger(event: string, detail?: any) {
    this.listeners[event]?.({ detail });
  }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

function setupRFB(): MockRFB | null {
  let lastInstance: MockRFB | null = null;
  (window as any).RFB = class extends MockRFB {
    constructor(target: HTMLElement, url: string) {
      super(target, url);
      lastInstance = this;
    }
  };
  return lastInstance; // will be set after connect() is called
}

// ── Tests ────────────────────────────────────────────────────────────────────

describe('VncService', () => {
  let service: VncService;

  beforeEach(() => {
    delete (window as any).RFB;
    TestBed.configureTestingModule({});
    service = TestBed.inject(VncService);
  });

  afterEach(() => {
    delete (window as any).RFB;
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  // ── Initial status ─────────────────────────────────────────────────────────

  describe('initial state', () => {
    it('should start with DISCONNECTED status', async () => {
      const status = await firstValueFrom(service.status$);
      expect(status).toBe(VNCConnectionStatus.DISCONNECTED);
    });
  });

  // ── disconnect ─────────────────────────────────────────────────────────────

  describe('disconnect', () => {
    it('should emit DISCONNECTED when called without an active connection', async () => {
      service.disconnect();
      const status = await firstValueFrom(service.status$);
      expect(status).toBe(VNCConnectionStatus.DISCONNECTED);
    });

    it('should not throw when called multiple times without a connection', () => {
      expect(() => {
        service.disconnect();
        service.disconnect();
      }).not.toThrow();
    });
  });

  // ── sendCtrlAltDel / sendKey – no-op when not connected ───────────────────

  describe('sendCtrlAltDel', () => {
    it('should not throw when rfb is null', () => {
      expect(() => service.sendCtrlAltDel()).not.toThrow();
    });
  });

  describe('sendKey', () => {
    it('should not throw when rfb is null', () => {
      expect(() => service.sendKey(65, true)).not.toThrow();
    });
  });

  // ── connect ────────────────────────────────────────────────────────────────

  describe('connect', () => {
    it('should emit CONNECTING immediately when connect() is called', async () => {
      // Use a "never resolves" mock so we can observe the CONNECTING state
      (window as any).RFB = undefined; // RFB not available yet → stays CONNECTING
      const statusAfterConnecting = firstValueFrom(service.status$.pipe(skip(1)));
      // Don't await connect() – it would hang waiting for RFB
      service.connect(document.createElement('div'), { url: 'ws://localhost:5900' }).catch(() => {});
      const status = await statusAfterConnecting;
      expect(status).toBe(VNCConnectionStatus.CONNECTING);
    });

    it('should reject and emit ERROR when RFB is not available within timeout', async () => {
      (window as any).RFB = undefined;

      // Use fake timers to skip the 10-second RFB wait
      vi.useFakeTimers();
      const connectPromise = service.connect(document.createElement('div'), { url: 'ws://localhost:5900' });
      vi.advanceTimersByTime(11_000);
      vi.useRealTimers();

      await expect(connectPromise).rejects.toThrow();
      const status = await firstValueFrom(service.status$);
      expect(status).toBe(VNCConnectionStatus.ERROR);
    });

    it('should set CONNECTED status when RFB fires the connect event', async () => {
      let rfbInstance: MockRFB | null = null;
      (window as any).RFB = class extends MockRFB {
        constructor(target: HTMLElement, url: string) {
          super(target, url);
          rfbInstance = this;
        }
      };

      const connectPromise = service.connect(document.createElement('div'), {
        url: 'ws://localhost:5900',
      });

      // Give the microtask queue a chance to set up listeners
      await Promise.resolve();
      rfbInstance!.trigger('connect');

      await connectPromise;
      const status = await firstValueFrom(service.status$);
      expect(status).toBe(VNCConnectionStatus.CONNECTED);
    });

    it('should set DISCONNECTED status when RFB fires the disconnect event', async () => {
      let rfbInstance: MockRFB | null = null;
      (window as any).RFB = class extends MockRFB {
        constructor(target: HTMLElement, url: string) {
          super(target, url);
          rfbInstance = this;
        }
      };

      service.connect(document.createElement('div'), { url: 'ws://localhost:5900' }).catch(() => {});
      await Promise.resolve();
      rfbInstance!.trigger('disconnect', { clean: true });

      const status = await firstValueFrom(service.status$);
      expect(status).toBe(VNCConnectionStatus.DISCONNECTED);
    });

    it('should set ERROR status when RFB fires the securityfailure event', async () => {
      let rfbInstance: MockRFB | null = null;
      (window as any).RFB = class extends MockRFB {
        constructor(target: HTMLElement, url: string) {
          super(target, url);
          rfbInstance = this;
        }
      };

      service.connect(document.createElement('div'), { url: 'ws://localhost:5900' }).catch(() => {});
      await Promise.resolve();
      rfbInstance!.trigger('securityfailure', { status: 1, reason: 'auth failed' });

      const status = await firstValueFrom(service.status$);
      expect(status).toBe(VNCConnectionStatus.ERROR);
    });

    it('should apply scaleViewport and resizeSession options on the RFB instance', async () => {
      let rfbInstance: MockRFB | null = null;
      (window as any).RFB = class extends MockRFB {
        constructor(target: HTMLElement, url: string) {
          super(target, url);
          rfbInstance = this;
        }
      };

      service
        .connect(document.createElement('div'), {
          url: 'ws://localhost:5900',
          scaleViewport: true,
          resizeSession: true,
        })
        .catch(() => {});

      await Promise.resolve();
      expect(rfbInstance!.scaleViewport).toBe(true);
      expect(rfbInstance!.resizeSession).toBe(true);
    });

    it('should send credentials when credentialsrequired fires and password is provided', async () => {
      let rfbInstance: any = null;
      const sendCredentialsMock = vi.fn();
      (window as any).RFB = class extends MockRFB {
        constructor(target: HTMLElement, url: string) {
          super(target, url);
          rfbInstance = this;
          (this as any).sendCredentials = sendCredentialsMock;
        }
      };

      service
        .connect(document.createElement('div'), {
          url: 'ws://localhost:5900',
          password: 'secret',
        })
        .catch(() => {});

      await Promise.resolve();
      rfbInstance!.trigger('credentialsrequired');
      expect(sendCredentialsMock).toHaveBeenCalledWith({ password: 'secret' });
    });

    it('should disconnect existing connection before creating a new one', async () => {
      const disconnectSpy = vi.fn();
      let callCount = 0;
      (window as any).RFB = class extends MockRFB {
        constructor(target: HTMLElement, url: string) {
          super(target, url);
          callCount++;
          (this as any).disconnect = disconnectSpy;
        }
      };

      const el = document.createElement('div');
      service.connect(el, { url: 'ws://localhost:5900' }).catch(() => {});
      await Promise.resolve();

      // Second connect should disconnect the first
      service.connect(el, { url: 'ws://localhost:5901' }).catch(() => {});
      await Promise.resolve();

      expect(disconnectSpy).toHaveBeenCalledTimes(1);
      expect(callCount).toBe(2);
    });
  });

  // ── disconnect with active connection ──────────────────────────────────────

  describe('disconnect after connect', () => {
    it('should emit DISCONNECTED after disconnect() is called on an active connection', async () => {
      let rfbInstance: MockRFB | null = null;
      const disconnectMock = vi.fn();
      (window as any).RFB = class extends MockRFB {
        constructor(target: HTMLElement, url: string) {
          super(target, url);
          rfbInstance = this;
          (this as any).disconnect = disconnectMock;
        }
      };

      service.connect(document.createElement('div'), { url: 'ws://localhost:5900' }).catch(() => {});
      await Promise.resolve();
      rfbInstance!.trigger('connect');

      service.disconnect();

      expect(disconnectMock).toHaveBeenCalled();
      const status = await firstValueFrom(service.status$);
      expect(status).toBe(VNCConnectionStatus.DISCONNECTED);
    });
  });

  // ── sendCtrlAltDel / sendKey with active connection ────────────────────────

  describe('sendCtrlAltDel with active connection', () => {
    it('should call rfb.sendCtrlAltDel()', async () => {
      const sendCtrlAltDelMock = vi.fn();
      let rfbInstance: any = null;
      (window as any).RFB = class extends MockRFB {
        constructor(target: HTMLElement, url: string) {
          super(target, url);
          rfbInstance = this;
          (this as any).sendCtrlAltDel = sendCtrlAltDelMock;
        }
      };

      service.connect(document.createElement('div'), { url: 'ws://localhost:5900' }).catch(() => {});
      await Promise.resolve();
      service.sendCtrlAltDel();

      expect(sendCtrlAltDelMock).toHaveBeenCalled();
    });
  });

  describe('sendKey with active connection', () => {
    it('should call rfb.sendKey() with the given keysym and down state', async () => {
      const sendKeyMock = vi.fn();
      let rfbInstance: any = null;
      (window as any).RFB = class extends MockRFB {
        constructor(target: HTMLElement, url: string) {
          super(target, url);
          rfbInstance = this;
          (this as any).sendKey = sendKeyMock;
        }
      };

      service.connect(document.createElement('div'), { url: 'ws://localhost:5900' }).catch(() => {});
      await Promise.resolve();
      service.sendKey(65, true);

      expect(sendKeyMock).toHaveBeenCalledWith(65, true);
    });
  });
});
