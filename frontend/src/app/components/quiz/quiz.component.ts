import { Component, OnInit, inject, signal, computed } from '@angular/core';
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
  taskId: number;
  frage: string;
  antworten: Antwort[] | string;
  bestehgrenzeProzent: number;
  maximalpunkte: number;
}

interface Task {
  id: number;
  title: string;
}

@Component({
  selector: 'app-quiz',
  imports: [CommonModule, FormsModule],
  templateUrl: './quiz.component.html',
  styleUrl: './quiz.component.css'
})
export class QuizComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  protected task = signal<Task | null>(null);
  protected questions = signal<Question[]>([]);
  protected currentQuestionIndex = signal(0);
  protected selectedAnswerIndices = signal<Set<number>>(new Set());
  protected feedback = signal<'correct' | 'wrong' | null>(null);
  protected isLoading = signal(true);
  protected showResults = signal(false);
  protected correctCount = signal(0);
  protected textAnswer = signal('');

  protected currentQuestion = computed(() => {
    const qs = this.questions();
    const idx = this.currentQuestionIndex();
    return qs[idx] ?? null;
  });

  protected parsedAnswers = computed<Antwort[]>(() => {
    const q = this.currentQuestion();
    if (!q) return [];
    if (typeof q.antworten === 'string') {
      try { return JSON.parse(q.antworten) as Antwort[]; } catch { return []; }
    }
    return q.antworten as Antwort[];
  });

  protected progress = computed(() => {
    const total = this.questions().length;
    if (total === 0) return 0;
    return ((this.currentQuestionIndex() + 1) / total) * 100;
  });

  async ngOnInit(): Promise<void> {
    const taskId = this.route.snapshot.paramMap.get('id');
    if (!taskId) {
      this.router.navigate(['/exercises']);
      return;
    }

    try {
      const [taskRes, questionsRes] = await Promise.all([
        apiClient.get<Task>(`/api/tasks/${taskId}`),
        apiClient.get<Question[]>(`/api/questions/task/${taskId}`)
      ]);
      this.task.set(taskRes.data);
      this.questions.set(questionsRes.data ?? []);

      if (this.questions().length === 0) {
        this.router.navigate(['/exercises']);
      }
    } catch {
      this.router.navigate(['/exercises']);
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
      this.correctCount.set(this.correctCount() + 1);
    } else {
      this.feedback.set('wrong');
    }
  }

  nextQuestion(): void {
    this.feedback.set(null);
    this.selectedAnswerIndices.set(new Set());
    this.textAnswer.set('');
    const next = this.currentQuestionIndex() + 1;
    if (next < this.questions().length) {
      this.currentQuestionIndex.set(next);
    } else {
      this.showResults.set(true);
    }
  }

  retryAnswer(): void {
    this.feedback.set(null);
    this.selectedAnswerIndices.set(new Set());
    this.textAnswer.set('');
  }

  restartQuiz(): void {
    this.currentQuestionIndex.set(0);
    this.selectedAnswerIndices.set(new Set());
    this.feedback.set(null);
    this.correctCount.set(0);
    this.showResults.set(false);
    this.textAnswer.set('');
  }

  exitQuiz(): void {
    this.router.navigate(['/exercises']);
  }
}
