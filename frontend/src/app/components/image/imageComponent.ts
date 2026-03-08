import { Component, OnInit, OnDestroy, ElementRef, ViewChild, signal, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { Image } from '../../models/Image';
import { Quiz } from '../../models/quiz.model';
import { VncService, VNCConnectionStatus } from '../../service/vnc.service';
import { ContainerControlService } from '../../service/container-control.service';
import { ExerciseService } from '../../service/exercise.service';
import { QuizService } from '../../services/quiz.service';
import axios, { AxiosResponse } from 'axios';

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
  sidebarOpen = signal(true);

  private expandedExercises = signal<Set<string>>(new Set());

  private route = inject(ActivatedRoute);
  private vncService = inject(VncService);
  private containerControlService = inject(ContainerControlService);
  private exerciseService = inject(ExerciseService);
  private quizService = inject(QuizService);

  protected exercises = this.exerciseService.getExercises();

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      const id = params['id'];
      this.imageId.set(id);
      this.loadImageData(id);
    });

    this.vncService.status$.subscribe((status: VNCConnectionStatus) => {
      this.connectionStatus.set(status);
      this.isConnecting.set(status === VNCConnectionStatus.CONNECTING);
    });
  }

  ngOnDestroy(): void {
    this.disconnect();
    this.containerControlService.clearSelectedImage();
  }

  toggleSidebar(): void {
    this.sidebarOpen.update(open => !open);
  }

  toggleExercise(exerciseId: string): void {
    this.expandedExercises.update(set => {
      const next = new Set(set);
      if (next.has(exerciseId)) {
        next.delete(exerciseId);
      } else {
        next.add(exerciseId);
      }
      return next;
    });
  }

  isExpanded(exerciseId: string): boolean {
    return this.expandedExercises().has(exerciseId);
  }

  getQuiz(exerciseId: string): Quiz | undefined {
    return this.quizService.getQuizByExerciseId(exerciseId);
  }

  private loadImageData(id: string): void {
    const mockImages: Image[] = [
      { ID: BigInt(1), Name: 'Ubuntu 22.04', URL: 'ubuntu:22.04' },
      { ID: BigInt(2), Name: 'Nginx Latest', URL: 'nginx:latest' },
      { ID: BigInt(3), Name: 'Node.js 20', URL: 'node:20' },
    ];

    const foundImage = mockImages.find(img => img.ID.toString() === id);
    if (foundImage) {
      this.image.set(foundImage);
      this.containerControlService.setSelectedImage(foundImage);
      setTimeout(() => this.connectVNC(), 500);
    }
  }

  private async connectVNC(): Promise<void> {
    if (!this.vncScreen) {
      console.error('VNC Screen element not found');
      return;
    }
    const userid = 1;
    const imagePort: AxiosResponse<VncInfoResponse, VncInfoResponse> = await axios.get(
      `http://localhost:9090/api/live-environment/vnc-port/${userid}`, {}
    );

    const vncUrl = `ws://localhost:9090/ws/novnc?vncPort=${imagePort.data.vncPort}`;

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
