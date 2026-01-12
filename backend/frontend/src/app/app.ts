import { Component, signal, OnInit } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { CommonModule } from '@angular/common';
import { ContainerControlService, ContainerControlState } from './service/container-control.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, CommonModule],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App implements OnInit {
  protected readonly title = signal('ITSI Container');
  isDarkMode = signal(false);
  containerControlState = signal<ContainerControlState>({
    selectedImage: null,
    isActive: false
  });
  isProcessing = signal(false);

  constructor(private containerControlService: ContainerControlService) {}

  ngOnInit(): void {
    this.containerControlService.state$.subscribe(state => {
      this.containerControlState.set(state);
    });
  }

  toggleTheme(): void {
    this.isDarkMode.update(current => !current);
    document.body.classList.toggle('dark-mode');
  }

  async startContainer(): Promise<void> {
    const state = this.containerControlState();
    if (!state.selectedImage || this.isProcessing()) return;

    this.isProcessing.set(true);
    try {
      await this.containerControlService.startContainer(1, Number(state.selectedImage.ID));
    } catch (error) {
      console.error('Fehler beim Starten:', error);
    } finally {
      this.isProcessing.set(false);
    }
  }

  async stopContainer(): Promise<void> {
    const state = this.containerControlState();
    if (!state.selectedImage || this.isProcessing()) return;

    this.isProcessing.set(true);
    try {
      await this.containerControlService.stopContainer(1, Number(state.selectedImage.ID));
    } catch (error) {
      console.error('Fehler beim Stoppen:', error);
    } finally {
      this.isProcessing.set(false);
    }
  }

  async restartContainer(): Promise<void> {
    const state = this.containerControlState();
    if (!state.selectedImage || this.isProcessing()) return;

    this.isProcessing.set(true);
    try {
      await this.containerControlService.restartContainer(1, Number(state.selectedImage.ID));
    } catch (error) {
      console.error('Fehler beim Neustarten:', error);
    } finally {
      this.isProcessing.set(false);
    }
  }
}
