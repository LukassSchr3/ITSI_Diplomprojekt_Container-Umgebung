import { Component, OnInit, signal, inject, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import apiClient from '../../service/api.service';

interface Antwort {
  text: string;
  richtig: boolean;
  punkte: number;
}

interface Question {
  id: number;
  taskId: number;
  frage: string;
  antworten: Antwort[] | string;
  bestehgrenzeProzent: number;
  maximalpunkte: number;
}

interface Task {
  id: number;
  title: string;
  description?: string;
  points: number;
  imageId: number;
}

@Component({
  selector: 'app-task-detail',
  imports: [CommonModule],
  templateUrl: './task-detail.html',
  styleUrl: './task-detail.css',
})
export class TaskDetail implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  task = signal<Task | null>(null);
  questions = signal<Question[]>([]);
  currentIndex = signal(0);
  selectedAnswerIndex = signal<number | null>(null);
  feedback = signal<'correct' | 'wrong' | null>(null);
  isLoading = signal(true);
  errorMessage = signal<string | null>(null);

  protected currentQuestion = computed(() => this.questions()[this.currentIndex()] ?? null);
  protected parsedAnswers = computed<Antwort[]>(() => {
    const q = this.currentQuestion();
    if (!q) return [];
    if (typeof q.antworten === 'string') {
      try { return JSON.parse(q.antworten) as Antwort[]; } catch { return []; }
    }
    return q.antworten as Antwort[];
  });
  protected allAnswered = computed(() =>
    this.questions().length > 0 && this.currentIndex() >= this.questions().length - 1 && this.feedback() === 'correct'
  );
  protected progressPercent = computed(() => {
    const total = this.questions().length;
    return total > 0 ? Math.round((this.currentIndex() / total) * 100) : 0;
  });

  async ngOnInit(): Promise<void> {
    const taskId = this.route.snapshot.paramMap.get('taskId');
    if (!taskId) { this.router.navigate(['/dashboard']); return; }
    await this.loadTaskAndQuestions(taskId);
  }

  private async loadTaskAndQuestions(taskId: string): Promise<void> {
    this.isLoading.set(true);
    try {
      const taskRes = await apiClient.get<Task>(`/api/tasks/${taskId}`);
      this.task.set(taskRes.data);

      const questionsRes = await apiClient.get<Question[]>(`/api/questions/task/${taskId}`);
      // Keine Fragen → Seite trotzdem anzeigen, nur ohne Fragenbereich
      this.questions.set(questionsRes.data ?? []);
    } catch {
      this.errorMessage.set('Aufgabe konnte nicht geladen werden.');
    } finally {
      this.isLoading.set(false);
    }
  }

  selectAnswer(index: number): void {
    if (this.feedback()) return;
    this.selectedAnswerIndex.set(index);
  }

  submitAnswer(): void {
    const idx = this.selectedAnswerIndex();
    if (idx === null) return;
    const answers = this.parsedAnswers();
    const chosen = answers[idx];
    if (chosen?.richtig) {
      this.feedback.set('correct');
    } else {
      this.feedback.set('wrong');
    }
  }

  nextQuestion(): void {
    this.feedback.set(null);
    this.selectedAnswerIndex.set(null);
    const next = this.currentIndex() + 1;
    if (next < this.questions().length) {
      this.currentIndex.set(next);
    }
  }

  retryAnswer(): void {
    this.feedback.set(null);
    this.selectedAnswerIndex.set(null);
  }

  navigateToImage(): void {
    const t = this.task();
    if (t?.imageId) {
      this.router.navigate(['/image', t.imageId]);
    } else {
      this.router.navigate(['/dashboard']);
    }
  }

  goBack(): void {
    this.router.navigate(['/dashboard']);
  }
}
