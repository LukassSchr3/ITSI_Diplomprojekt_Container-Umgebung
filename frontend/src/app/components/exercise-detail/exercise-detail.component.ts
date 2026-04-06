import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import apiClient from '../../service/api.service';

interface Task {
  id: number;
  title: string;
  description?: string;
  points: number;
  imageId: number;
}

@Component({
  selector: 'app-exercise-detail',
  imports: [CommonModule, RouterLink],
  templateUrl: './exercise-detail.component.html',
  styleUrl: './exercise-detail.component.css'
})
export class ExerciseDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  task = signal<Task | null>(null);
  isLoading = signal(true);
  notFound = signal(false);

  async ngOnInit(): Promise<void> {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.notFound.set(true);
      this.isLoading.set(false);
      return;
    }
    try {
      const res = await apiClient.get<Task>(`/api/tasks/${id}`);
      this.task.set(res.data);
    } catch {
      this.notFound.set(true);
    } finally {
      this.isLoading.set(false);
    }
  }

  startLiveEnv(): void {
    const t = this.task();
    if (t?.imageId) {
      this.router.navigate(['/image', t.imageId]);
    }
  }

  startQuiz(): void {
    const t = this.task();
    if (t) {
      this.router.navigate(['/quiz-start', t.id]);
    }
  }

  goBack(): void {
    this.router.navigate(['/exercises']);
  }
}
