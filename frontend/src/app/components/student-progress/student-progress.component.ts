import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../service/auth.service';
import apiClient from '../../service/api.service';

interface DashboardTask {
  id: number;
  title: string;
  description?: string;
  points: number;
  imageId: number;
}

interface DashboardCourse {
  courseId: number;
  courseName: string;
  courseDescription?: string;
  tasks: DashboardTask[];
}

interface QuestionResult {
  id: number;
  userId: number;
  questionId: number;
  erreichtePunkte: number;
  bestanden: boolean;
}

interface Question {
  id: number;
  taskId: number;
  maximalpunkte: number;
}

interface TaskProgress {
  task: DashboardTask;
  answeredCount: number;
  totalQuestions: number;
  erreichtePunkte: number;
  maxPunkte: number;
  percent: number;
}

interface CourseProgress {
  course: DashboardCourse;
  tasks: TaskProgress[];
  totalPercent: number;
}

@Component({
  selector: 'app-student-progress',
  imports: [CommonModule],
  templateUrl: './student-progress.component.html',
  styleUrl: './student-progress.component.css'
})
export class StudentProgressComponent implements OnInit {
  private authService = inject(AuthService);
  private router = inject(Router);

  protected courseProgress = signal<CourseProgress[]>([]);
  protected isLoading = signal(true);
  protected errorMessage = signal<string | null>(null);

  async ngOnInit(): Promise<void> {
    await this.loadProgress();
  }

  private async loadProgress(): Promise<void> {
    this.isLoading.set(true);
    try {
      const userId = this.authService.getUserId()();
      if (!userId) {
        this.errorMessage.set('Benutzer-ID nicht gefunden.');
        return;
      }

      // Load courses + questions + results in parallel
      const [coursesRes, resultsRes] = await Promise.all([
        apiClient.get<DashboardCourse[]>(`/api/student-courses/user/${userId}/dashboard`),
        apiClient.get<QuestionResult[]>(`/api/question-results/user/${userId}`)
      ]);

      const courses = coursesRes.data ?? [];
      const results = resultsRes.data ?? [];

      // Build a map: questionId → result
      const resultMap = new Map<number, QuestionResult>();
      for (const r of results) {
        resultMap.set(r.questionId, r);
      }

      // For each course/task, load questions and compute progress
      const progress: CourseProgress[] = await Promise.all(
        courses.map(async (course) => {
          const taskProgressList: TaskProgress[] = await Promise.all(
            course.tasks.map(async (task) => {
              const questionsRes = await apiClient.get<Question[]>(`/api/questions/task/${task.id}`);
              const questions = questionsRes.data ?? [];

              let erreicht = 0;
              let maxPunkte = 0;
              let answered = 0;

              for (const q of questions) {
                maxPunkte += q.maximalpunkte;
                const result = resultMap.get(q.id);
                if (result?.bestanden) {
                  answered++;
                  erreicht += result.erreichtePunkte;
                }
              }

              return {
                task,
                answeredCount: answered,
                totalQuestions: questions.length,
                erreichtePunkte: erreicht,
                maxPunkte,
                percent: maxPunkte > 0 ? Math.round((erreicht / maxPunkte) * 100) : 0
              } as TaskProgress;
            })
          );

          const totalMax = taskProgressList.reduce((sum, tp) => sum + tp.maxPunkte, 0);
          const totalErreicht = taskProgressList.reduce((sum, tp) => sum + tp.erreichtePunkte, 0);

          return {
            course,
            tasks: taskProgressList,
            totalPercent: totalMax > 0 ? Math.round((totalErreicht / totalMax) * 100) : 0
          } as CourseProgress;
        })
      );

      this.courseProgress.set(progress);
    } catch {
      this.errorMessage.set('Fortschritt konnte nicht geladen werden.');
    } finally {
      this.isLoading.set(false);
    }
  }

  goBack(): void {
    this.router.navigate(['/dashboard']);
  }
}
