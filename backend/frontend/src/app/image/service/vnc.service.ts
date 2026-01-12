import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import axios from 'axios';

declare global {
  interface Window {
    RFB: any;
  }
}

export enum VNCConnectionStatus {
  DISCONNECTED = 'Getrennt',
  CONNECTING = 'Verbindung wird hergestellt...',
  CONNECTED = 'Verbunden',
  ERROR = 'Fehler'
}

export interface VNCOptions {
  url: string;
  password?: string;
  scaleViewport?: boolean;
  resizeSession?: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class VncService {
  private rfb: any = null;
  private statusSubject = new BehaviorSubject<VNCConnectionStatus>(VNCConnectionStatus.DISCONNECTED);
  public status$: Observable<VNCConnectionStatus> = this.statusSubject.asObservable();

  constructor() {}

  private async loadRFB(): Promise<any> {
    if (window.RFB) {
      return window.RFB;
    }

    // Warte bis RFB verfügbar ist
    return new Promise((resolve, reject) => {
      const checkInterval = setInterval(() => {
        if (window.RFB) {
          clearInterval(checkInterval);
          resolve(window.RFB);
        }
      }, 100);

      setTimeout(() => {
        clearInterval(checkInterval);
        reject(new Error('RFB konnte nicht geladen werden'));
      }, 10000);
    });
  }

  async connect(target: HTMLElement, options: VNCOptions): Promise<void> {
    try {
      this.statusSubject.next(VNCConnectionStatus.CONNECTING);

      // Bestehende Verbindung trennen falls vorhanden
      if (this.rfb) {
        this.disconnect();
      }

      // RFB Klasse laden
      const RFB = await this.loadRFB();

      // Neue RFB Instanz erstellen (API für noVNC 1.3.0)
      this.rfb = new RFB(target, options.url);

      // Optionen anwenden
      if (options.scaleViewport !== undefined) {
        this.rfb.scaleViewport = options.scaleViewport;
      }
      if (options.resizeSession !== undefined) {
        this.rfb.resizeSession = options.resizeSession;
      }

      // Event-Listener registrieren
      this.rfb.addEventListener('connect', () => {
        console.log('VNC verbunden');
        this.statusSubject.next(VNCConnectionStatus.CONNECTED);
      });

      this.rfb.addEventListener('disconnect', (event: any) => {
        console.log('VNC getrennt:', event.detail);
        this.statusSubject.next(VNCConnectionStatus.DISCONNECTED);
        this.rfb = null;
      });

      this.rfb.addEventListener('credentialsrequired', () => {
        console.log('VNC Passwort erforderlich');
        if (options.password) {
          this.rfb.sendCredentials({ password: options.password });
        }
      });

      this.rfb.addEventListener('securityfailure', (event: any) => {
        console.error('VNC Sicherheitsfehler:', event.detail);
        this.statusSubject.next(VNCConnectionStatus.ERROR);
      });


    } catch (error) {
      console.error('Fehler beim Verbinden zu VNC:', error);
      this.statusSubject.next(VNCConnectionStatus.ERROR);
      throw error;
    }
  }

  disconnect(): void {
    if (this.rfb) {
      try {
        this.rfb.disconnect();
      } catch (error) {
        console.error('Fehler beim Trennen:', error);
      }
      this.rfb = null;
    }
    this.statusSubject.next(VNCConnectionStatus.DISCONNECTED);
  }

  sendCtrlAltDel(): void {
    if (this.rfb) {
      this.rfb.sendCtrlAltDel();
    }
  }

  sendKey(keysym: number, down: boolean): void {
    if (this.rfb) {
      this.rfb.sendKey(keysym, down);
    }
  }
}

