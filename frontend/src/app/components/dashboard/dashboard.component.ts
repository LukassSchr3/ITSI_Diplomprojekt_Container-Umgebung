import { Component, inject, computed, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { ExerciseService } from '../../service/exercise.service';
import { AuthService } from '../../service/auth.service';
import { PermissionService } from '../../service/permission.service';
import { Exercise } from '../../models/exercise.model';
import { DashboardCourse, CourseTask } from '../../models/DashboardCourse';
import { StudentCourse } from '../../models/StudentCourse';
import apiClient from '../../service/api.service';

@Component({
  selector: 'app-dashboard',
  imports: [CommonModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit {
  private exerciseService = inject(ExerciseService);
  private authService = inject(AuthService);
  private router = inject(Router);
  private permissions = inject(PermissionService);

  protected exercises = this.exerciseService.getExercises();
  protected roles = this.authService.getRoles();
  protected canGrade = this.permissions.canGrade;
  protected canWrite = this.permissions.canWrite;

  // Kurs-Daten
  protected dashboardCourses = signal<DashboardCourse[]>([]);
  protected isLoading = signal(false);
  protected errorMessage = signal<string | null>(null);

  protected isAdmin = computed(() => this.roles().includes('ADMIN'));

  protected roleLabel = computed(() => {
    const roles = this.roles();
    if (roles.includes('ADMIN')) return 'Admin';
    if (roles.includes('LEHRER')) return 'Lehrer';
    if (roles.includes('SCHUELER')) return 'Schueler';
    return 'Unbekannt';
  });

  protected stats = computed(() => {
    const all = this.exercises();
    const notStarted = all.filter(e => e.status === 'not-started').length;
    const inProgress = all.filter(e => e.status === 'in-progress').length;
    const completed = all.filter(e => e.status === 'completed').length;
    const total = all.length;

    return { notStarted, inProgress, completed, total };
  });

  async ngOnInit(): Promise<void> {
    await this.loadCourses();
  }

  private async loadCourses(): Promise<void> {
    const userId = this.authService.getUserId()();
    if (!userId) {
      this.errorMessage.set('Kein Benutzer angemeldet.');
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set(null);

    try {
      const studentCoursesRes = await apiClient.get<StudentCourse[]>(
        `/api/student-courses/user/${userId}`
      );
      const studentCourses = studentCoursesRes.data ?? [];

      const dashboardCourses = await Promise.all(
        studentCourses.map(async (sc) => {
          const [courseRes, tasksRes] = await Promise.all([
            apiClient.get<{ id: number; name: string }>(`/api/courses/${sc.courseId}`),
            apiClient.get<CourseTask[]>(`/api/course-tasks/course/${sc.courseId}/tasks`)
          ]);

          const course: DashboardCourse = {
            courseId: sc.courseId,
            courseName: courseRes.data?.name ?? `Kurs ${sc.courseId}`,
            enrolledAt: sc.enrolledAt,
            expiresAt: sc.expiresAt,
            tasks: tasksRes.data ?? []
          };
          return course;
        })
      );

      this.dashboardCourses.set(dashboardCourses);
    } catch {
      this.errorMessage.set('Die Kursdaten konnten nicht geladen werden. Bitte versuche es spater erneut.');
    } finally {
      this.isLoading.set(false);
    }
  }

  onStatusChange(id: string, newStatus: 'not-started' | 'in-progress' | 'completed') {
    if (!this.canWrite()) return;
    this.exerciseService.updateStatus(id, newStatus);
  }

  onExerciseClick(exercise: Exercise) {
    console.log('Exercise clicked:', exercise.id, 'imageId:', exercise.imageId);
    if (exercise.imageId) {
      this.router.navigate(['/image', exercise.imageId]);
    } else {
      console.warn('No imageId defined for exercise:', exercise.id);
    }
  }

  onTaskClick(task: CourseTask) {
    if (task.id) {
      this.router.navigate(['/task', task.id]);
    }
  }

  onBewerten(id: string) {
    if (!this.canGrade()) return;
    this.exerciseService.markBewertet(id);
  }

  goToAdmin() {
    this.router.navigate(['/admin']);
  }

  logout() {
    this.authService.logout();
  }
}
