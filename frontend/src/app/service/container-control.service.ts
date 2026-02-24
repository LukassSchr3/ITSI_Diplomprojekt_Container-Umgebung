import { Injectable, inject } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { Image } from '../models/Image';
import axios from 'axios';
import { AuthService } from './auth.service';

export interface ContainerControlState {
  selectedImage: Image | null;
  isActive: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class ContainerControlService {
  baseURL = 'http://localhost:9090/api/container';
  private authService = inject(AuthService);

  private stateSubject = new BehaviorSubject<ContainerControlState>({
    selectedImage: null,
    isActive: false
  });

  public state$: Observable<ContainerControlState> = this.stateSubject.asObservable();

  setSelectedImage(image: Image | null): void {
    this.stateSubject.next({
      selectedImage: image,
      isActive: image !== null
    });
  }

  clearSelectedImage(): void {
    this.stateSubject.next({
      selectedImage: null,
      isActive: false
    });
  }

  private authHeaders() {
    const token = this.authService.getToken()();
    return token ? { Authorization: `Bearer ${token}` } : {};
  }

  async startContainer(userId: number, imageId: number): Promise<void> {
    try {
      await axios.post(
        `${this.baseURL}/start`,
        { userId, imageId },
        { headers: this.authHeaders() }
      );
      console.log('Container gestartet');
    } catch (error) {
      console.error('Fehler beim Starten des Containers:', error);
      throw error;
    }
  }

  async stopContainer(userId: number, imageId: number): Promise<void> {
    try {
      await axios.post(
        `${this.baseURL}/stop`,
        { userId, imageId },
        { headers: this.authHeaders() }
      );
      console.log('Container gestoppt');
    } catch (error) {
      console.error('Fehler beim Stoppen des Containers:', error);
      throw error;
    }
  }

  async restartContainer(userId: number, imageId: number): Promise<void> {
    try {
      await axios.post(
        `${this.baseURL}/reset`,
        { userId, imageId },
        { headers: this.authHeaders() }
      );
      console.log('Container neu gestartet');
    } catch (error) {
      console.error('Fehler beim Neustarten des Containers:', error);
      throw error;
    }
  }
}
