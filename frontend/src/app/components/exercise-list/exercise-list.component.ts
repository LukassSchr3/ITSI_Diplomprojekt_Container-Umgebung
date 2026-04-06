import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../service/auth.service';
import apiClient from '../../service/api.service';

interface Task {
  id: number;
  title: string;
  description?: string;
  points: number;
  imageId: number;
}

@Component({
  selector: 'app-exercise-list',
  imports: [CommonModule, RouterLink],
  templateUrl: './exercise-list.component.html',
  styleUrl: './exercise-list.component.css'
})
export class ExerciseListComponent implements OnInit {
  private authService = inject(AuthService);
  private router = inject(Router);

  protected tasks = signal<Task[]>([]);
  protected isLoading = signal(true);
  protected isTeacher = this.authService.isTeacher();

  async ngOnInit(): Promise<void> {
    await this.loadTasks();
  }

  private async loadTasks(): Promise<void> {
    this.isLoading.set(true);
    try {
      const res = await apiClient.get<Task[]>('/api/tasks');
      this.tasks.set(res.data ?? []);
    } catch {
      this.tasks.set([]);
    } finally {
      this.isLoading.set(false);
    }
  }

  hasQuiz(taskId: number): boolean {
    // Questions are always loaded on the quiz page itself
    return true;
  }

  startQuiz(taskId: number): void {
    this.router.navigate(['/quiz-start', taskId]);
  }

  navigateTo(taskId: number): void {
    this.router.navigate(['/exercises', taskId]);
  }

  goBack() {
    this.router.navigate(['/dashboard']);
  }
}
