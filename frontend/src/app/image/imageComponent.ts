import { Component, OnInit, OnDestroy, ElementRef, ViewChild, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { Image } from '../interfaces/Image';
import { VncService, VNCConnectionStatus } from './service/vnc.service';
import { ContainerControlService } from '../service/container-control.service';
import axios, {AxiosResponse} from 'axios';


export interface VncInfoResponse {
  vncPort: number;
  vncPassword: string;
}

@Component({
  selector: 'app-image',
  imports: [CommonModule, RouterLink],
  templateUrl: './image.html',
  styleUrl: './image.css',
})
export class ImageComponent implements OnInit, OnDestroy {
  @ViewChild('vncScreen', { static: false }) vncScreen!: ElementRef<HTMLDivElement>;

  imageId = signal<string | null>(null);
  image = signal<Image | null>(null);
  isConnecting = signal(true);
  connectionStatus = signal('Verbindung wird hergestellt...');

  constructor(
    private route: ActivatedRoute,
    private vncService: VncService,
    private containerControlService: ContainerControlService
  ) {}

  ngOnInit(): void {
    // Route-Parameter abonnieren
    this.route.params.subscribe(params => {
      const id = params['id'];
      this.imageId.set(id);
      this.loadImageData(id);
    });

    // VNC-Status abonnieren
    this.vncService.status$.subscribe((status: VNCConnectionStatus) => {
      this.connectionStatus.set(status);
      this.isConnecting.set(status === VNCConnectionStatus.CONNECTING);
    });
  }

  ngOnDestroy(): void {
    this.disconnect();
    // Image-Auswahl zurücksetzen wenn Komponente zerstört wird
    this.containerControlService.clearSelectedImage();
  }

  private loadImageData(id: string): void {
    // Hier würdest du normalerweise die Daten vom Backend laden
    // Für Demo-Zwecke verwenden wir Mock-Daten
    const mockImages: Image[] = [
      { ID: BigInt(1), Name: 'Ubuntu 22.04', URL: 'ubuntu:22.04' },
      { ID: BigInt(2), Name: 'Nginx Latest', URL: 'nginx:latest' },
      { ID: BigInt(3), Name: 'Node.js 20', URL: 'node:20' },
    ];

    const foundImage = mockImages.find(img => img.ID.toString() === id);
    if (foundImage) {
      this.image.set(foundImage);
      // Image als ausgewählt registrieren
      this.containerControlService.setSelectedImage(foundImage);
      setTimeout(() => this.connectVNC(), 500);
    }
  }

  private async connectVNC(): Promise<void> {
    if (!this.vncScreen) {
      console.error('VNC Screen element not found');
      return;
    }
    const userid = 1; // Beispiel-Benutzer-ID
    const imagePort: AxiosResponse<VncInfoResponse, VncInfoResponse> = await axios.get(`http://localhost:9090/api/live-environment/vnc-port/${userid}`, {})

    console.log(imagePort.data.vncPort)

    // VNC WebSocket URL - passe diese an dein Backend an
    // Format: ws://host:port oder wss://host:port für verschlüsselte Verbindung
    const vncUrl = `ws://localhost:9090/ws/novnc?vncPort=${imagePort.data.vncPort}`; // Beispiel-URL

    this.vncService.connect(this.vncScreen.nativeElement, {
      url: vncUrl,
      password: imagePort.data.vncPassword,
      scaleViewport: true,
      resizeSession: true
    });
  }

  disconnect(): void {
    this.vncService.disconnect();
  }
}
