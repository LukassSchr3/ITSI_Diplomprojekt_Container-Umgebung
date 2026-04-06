import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import apiClient from '../../service/api.service';

interface Task {
  id: number;
  title: string;
  description?: string;
  points: number;
}

interface Question {
  id: number;
  taskId: number;
  frage: string;
}

@Component({
  selector: 'app-quiz-start',
  imports: [CommonModule],
  templateUrl: './quiz-start.component.html',
  styleUrl: './quiz-start.component.css'
})
export class QuizStartComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  protected task = signal<Task | null>(null);
  protected questionCount = signal(0);
  protected errorMessage = signal('');
  protected isLoading = signal(true);

  async ngOnInit(): Promise<void> {
    const taskId = this.route.snapshot.paramMap.get('exerciseId');
    if (!taskId) {
      this.errorMessage.set('Keine Aufgabe angegeben');
      this.isLoading.set(false);
      return;
    }

    try {
      const [taskRes, questionsRes] = await Promise.all([
        apiClient.get<Task>(`/api/tasks/${taskId}`),
        apiClient.get<Question[]>(`/api/questions/task/${taskId}`)
      ]);
      this.task.set(taskRes.data);
      this.questionCount.set(questionsRes.data?.length ?? 0);

      if (this.questionCount() === 0) {
        this.errorMessage.set('Keine Fragen für diese Aufgabe vorhanden');
      }
    } catch {
      this.errorMessage.set('Quiz konnte nicht geladen werden');
    } finally {
      this.isLoading.set(false);
    }
  }

  startQuiz(): void {
    const t = this.task();
    if (t) {
      this.router.navigate(['/quiz', t.id]);
    }
  }

  goBack(): void {
    this.router.navigate(['/exercises']);
  }
}
