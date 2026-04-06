import { Component, OnInit, OnDestroy, ElementRef, ViewChild, signal, inject, computed } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { VncService, VNCConnectionStatus } from '../../service/vnc.service';
import { AuthService } from '../../service/auth.service';
import apiClient from '../../service/api.service';
import axios, { AxiosResponse } from 'axios';

export interface VncInfoResponse {
  vncPort: number;
  vncPassword: string;
}

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

interface QuestionResult {
  id: number;
  userId: number;
  questionId: number;
  erreichtePunkte: number;
  bestanden: boolean;
}

interface TaskWithQuestions {
  task: Task;
  questions: Question[];
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
  isConnecting = signal(true);
  connectionStatus = signal('Verbindung wird hergestellt...');
  sidebarOpen = signal(true);

  // Backend data
  protected tasksWithQuestions = signal<TaskWithQuestions[]>([]);
  protected answeredQuestions = signal<Set<number>>(new Set());
  protected isLoadingTasks = signal(true);

  // Inline quiz state
  protected activeQuestionId = signal<number | null>(null);
  protected selectedAnswerIndex = signal<number | null>(null);
  protected questionFeedback = signal<'correct' | 'wrong' | null>(null);

  private expandedTasks = signal<Set<number>>(new Set());

  private route = inject(ActivatedRoute);
  private vncService = inject(VncService);
  private authService = inject(AuthService);

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      const id = params['id'];
      this.imageId.set(id);
      this.loadTasksAndConnect(id);
    });

    this.vncService.status$.subscribe((status: VNCConnectionStatus) => {
      this.connectionStatus.set(status);
      this.isConnecting.set(status === VNCConnectionStatus.CONNECTING);
    });
  }

  ngOnDestroy(): void {
    this.vncService.disconnect();
  }

  private async loadTasksAndConnect(imageId: string): Promise<void> {
    // Load tasks for this image + VNC in parallel
    await Promise.all([
      this.loadTasks(imageId),
      this.connectVNC()
    ]);
  }

  private async loadTasks(imageId: string): Promise<void> {
    this.isLoadingTasks.set(true);
    try {
      const userId = this.authService.getUserId()();

      // Load tasks for this image
      const tasksRes = await apiClient.get<Task[]>(`/api/tasks/image/${imageId}`);
      const tasks = tasksRes.data ?? [];

      // Load questions for each task in parallel
      const tasksWithQuestions = await Promise.all(
        tasks.map(async (task) => {
          const questionsRes = await apiClient.get<Question[]>(`/api/questions/task/${task.id}`);
          return { task, questions: questionsRes.data ?? [] } as TaskWithQuestions;
        })
      );
      this.tasksWithQuestions.set(tasksWithQuestions);

      // Load already answered questions
      if (userId) {
        const resultsRes = await apiClient.get<QuestionResult[]>(`/api/question-results/user/${userId}`);
        const answered = new Set((resultsRes.data ?? []).filter(r => r.bestanden).map(r => r.questionId));
        this.answeredQuestions.set(answered);
      }
    } catch (err) {
      console.error('Fehler beim Laden der Aufgaben:', err);
    } finally {
      this.isLoadingTasks.set(false);
    }
  }

  private async connectVNC(): Promise<void> {
    // Wait for ViewChild to be available
    await new Promise(resolve => setTimeout(resolve, 500));
    if (!this.vncScreen) {
      console.error('VNC Screen element not found');
      return;
    }

    const userId = this.authService.getUserId()();
    if (!userId) return;

    try {
      const imagePort: AxiosResponse<VncInfoResponse> = await axios.get(
        `http://localhost:9090/api/live-environment/vnc-port/${userId}`, {}
      );
      const vncUrl = `ws://localhost:9090/ws/novnc?vncPort=${imagePort.data.vncPort}`;
      this.vncService.connect(this.vncScreen.nativeElement, {
        url: vncUrl,
        password: imagePort.data.vncPassword,
        scaleViewport: true,
        resizeSession: true
      });
    } catch (err) {
      console.error('VNC Verbindung fehlgeschlagen:', err);
    }
  }

  // Sidebar accordion
  toggleSidebar(): void {
    this.sidebarOpen.update(open => !open);
  }

  toggleTask(taskId: number): void {
    this.expandedTasks.update(set => {
      const next = new Set(set);
      if (next.has(taskId)) {
        next.delete(taskId);
      } else {
        next.add(taskId);
      }
      return next;
    });
  }

  isExpanded(taskId: number): boolean {
    return this.expandedTasks().has(taskId);
  }

  getTaskProgress(tw: TaskWithQuestions): string {
    const total = tw.questions.length;
    if (total === 0) return '';
    const answered = tw.questions.filter(q => this.answeredQuestions().has(q.id)).length;
    return `${answered}/${total}`;
  }

  isQuestionAnswered(questionId: number): boolean {
    return this.answeredQuestions().has(questionId);
  }

  // Inline quiz
  activateQuestion(questionId: number): void {
    if (this.answeredQuestions().has(questionId)) return;
    this.activeQuestionId.set(questionId);
    this.selectedAnswerIndex.set(null);
    this.questionFeedback.set(null);
  }

  isQuestionActive(questionId: number): boolean {
    return this.activeQuestionId() === questionId;
  }

  getParsedAnswers(question: Question): Antwort[] {
    if (typeof question.antworten === 'string') {
      try { return JSON.parse(question.antworten) as Antwort[]; } catch { return []; }
    }
    return question.antworten as Antwort[];
  }

  selectAnswer(index: number): void {
    if (this.questionFeedback()) return;
    this.selectedAnswerIndex.set(index);
  }

  async submitAnswer(question: Question): Promise<void> {
    const idx = this.selectedAnswerIndex();
    if (idx === null) return;

    const answers = this.getParsedAnswers(question);
    const chosen = answers[idx];

    if (chosen?.richtig) {
      this.questionFeedback.set('correct');

      // Save to backend
      const userId = this.authService.getUserId()();
      if (userId) {
        try {
          await apiClient.post('/api/question-results', {
            userId: Number(userId),
            questionId: question.id,
            erreichtePunkte: chosen.punkte ?? question.maximalpunkte,
            bestanden: true
          });
          // Mark as answered in local state
          this.answeredQuestions.update(set => {
            const next = new Set(set);
            next.add(question.id);
            return next;
          });
        } catch (err) {
          console.error('Fehler beim Speichern:', err);
        }
      }
    } else {
      this.questionFeedback.set('wrong');
    }
  }

  retryQuestion(): void {
    this.questionFeedback.set(null);
    this.selectedAnswerIndex.set(null);
  }

  nextQuestion(taskQuestions: Question[]): void {
    const currentId = this.activeQuestionId();
    if (currentId === null) return;
    const currentIdx = taskQuestions.findIndex(q => q.id === currentId);
    // Find next unanswered question
    for (let i = currentIdx + 1; i < taskQuestions.length; i++) {
      if (!this.answeredQuestions().has(taskQuestions[i].id)) {
        this.activateQuestion(taskQuestions[i].id);
        return;
      }
    }
    // No more unanswered - close
    this.activeQuestionId.set(null);
    this.questionFeedback.set(null);
    this.selectedAnswerIndex.set(null);
  }
}
