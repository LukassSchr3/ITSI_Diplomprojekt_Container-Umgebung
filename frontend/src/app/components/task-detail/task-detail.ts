import { Component, OnInit, signal, inject, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
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
  imports: [CommonModule, FormsModule],
  templateUrl: './task-detail.html',
  styleUrl: './task-detail.css',
})
export class TaskDetail implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  task = signal<Task | null>(null);
  questions = signal<Question[]>([]);
  currentIndex = signal(0);
  selectedAnswerIndices = signal<Set<number>>(new Set());
  feedback = signal<'correct' | 'wrong' | null>(null);
  textAnswer = signal('');
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
    const taskId = this.route.snapshot.paramMap.get('id');
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

  isTextQuestion(): boolean {
    const answers = this.parsedAnswers();
    return answers.length === 1 && answers[0].richtig;
  }

  isMultiSelect(): boolean {
    const answers = this.parsedAnswers();
    return answers.filter(a => a.richtig).length > 1;
  }

  selectAnswer(index: number): void {
    if (this.feedback()) return;
    if (this.isMultiSelect()) {
      this.selectedAnswerIndices.update(set => {
        const next = new Set(set);
        if (next.has(index)) { next.delete(index); } else { next.add(index); }
        return next;
      });
    } else {
      this.selectedAnswerIndices.set(new Set([index]));
    }
  }

  submitAnswer(): void {
    const answers = this.parsedAnswers();
    let isCorrect = false;

    if (this.isTextQuestion()) {
      isCorrect = this.textAnswer().trim().toLowerCase() === answers[0].text.trim().toLowerCase();
    } else {
      const selected = this.selectedAnswerIndices();
      if (selected.size === 0) return;
      const correctIndices = new Set(answers.map((a, i) => a.richtig ? i : -1).filter(i => i >= 0));
      isCorrect = selected.size === correctIndices.size && [...selected].every(i => correctIndices.has(i));
    }

    if (isCorrect) {
      this.feedback.set('correct');
    } else {
      this.feedback.set('wrong');
    }
  }

  nextQuestion(): void {
    this.feedback.set(null);
    this.selectedAnswerIndices.set(new Set());
    this.textAnswer.set('');
    const next = this.currentIndex() + 1;
    if (next < this.questions().length) {
      this.currentIndex.set(next);
    }
  }

  retryAnswer(): void {
    this.feedback.set(null);
    this.selectedAnswerIndices.set(new Set());
    this.textAnswer.set('');
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
