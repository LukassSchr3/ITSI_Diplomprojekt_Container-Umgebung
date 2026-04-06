import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import apiClient from '../../service/api.service';

interface UserItem {
  id: number;
  name: string;
  email: string;
  className?: string;
  role?: string;
}

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
  courseName: string;
  tasks: TaskProgress[];
  totalPercent: number;
}

@Component({
  selector: 'app-teacher-progress',
  imports: [CommonModule, FormsModule],
  templateUrl: './teacher-progress.component.html',
  styleUrl: './teacher-progress.component.css'
})
export class TeacherProgressComponent implements OnInit {
  private router = inject(Router);

  // User search
  protected allUsers = signal<UserItem[]>([]);
  protected searchQuery = signal('');
  protected showDropdown = signal(false);
  protected selectedUser = signal<UserItem | null>(null);

  // Progress data for selected user
  protected courseProgress = signal<CourseProgress[]>([]);
  protected isLoadingProgress = signal(false);
  protected errorMessage = signal<string | null>(null);

  protected filteredUsers = computed(() => {
    const query = this.searchQuery().toLowerCase().trim();
    if (!query) return [];
    return this.allUsers()
      .filter(u =>
        u.name.toLowerCase().includes(query) ||
        u.email.toLowerCase().includes(query) ||
        (u.className?.toLowerCase().includes(query) ?? false)
      )
      .slice(0, 10);
  });

  async ngOnInit(): Promise<void> {
    try {
      const res = await apiClient.get<UserItem[]>('/api/users');
      this.allUsers.set(res.data ?? []);
    } catch {
      this.errorMessage.set('Benutzerliste konnte nicht geladen werden.');
    }
  }

  onSearchInput(value: string): void {
    this.searchQuery.set(value);
    this.showDropdown.set(true);
    this.selectedUser.set(null);
    this.courseProgress.set([]);
  }

  async selectUser(user: UserItem): Promise<void> {
    this.selectedUser.set(user);
    this.searchQuery.set(`${user.name} (${user.email})`);
    this.showDropdown.set(false);
    await this.loadProgressForUser(user.id);
  }

  hideDropdown(): void {
    // Delay to allow click on dropdown item
    setTimeout(() => this.showDropdown.set(false), 200);
  }

  private async loadProgressForUser(userId: number): Promise<void> {
    this.isLoadingProgress.set(true);
    this.errorMessage.set(null);
    try {
      const [coursesRes, resultsRes] = await Promise.all([
        apiClient.get<DashboardCourse[]>(`/api/student-courses/user/${userId}/dashboard`),
        apiClient.get<QuestionResult[]>(`/api/question-results/user/${userId}`)
      ]);

      const courses = coursesRes.data ?? [];
      const results = resultsRes.data ?? [];

      const resultMap = new Map<number, QuestionResult>();
      for (const r of results) {
        resultMap.set(r.questionId, r);
      }

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
            courseName: course.courseName,
            tasks: taskProgressList,
            totalPercent: totalMax > 0 ? Math.round((totalErreicht / totalMax) * 100) : 0
          } as CourseProgress;
        })
      );

      this.courseProgress.set(progress);
    } catch {
      this.errorMessage.set('Fortschritt konnte nicht geladen werden.');
    } finally {
      this.isLoadingProgress.set(false);
    }
  }

  goBack(): void {
    this.router.navigate(['/dashboard']);
  }
}
