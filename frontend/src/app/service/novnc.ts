import { Injectable } from '@angular/core';

declare global {
  interface Window {
    RFB: any;
  }
}

@Injectable({
  providedIn: 'root'
})
export class NoVncService {

  private async _loadRFB(): Promise<any> {
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

  async createRFBConnection(url: string, target: HTMLElement): Promise<any> {
    const RFB = await this._loadRFB();
    return new RFB(target, url);
  }
}

