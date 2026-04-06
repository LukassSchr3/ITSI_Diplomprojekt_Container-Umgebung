import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../service/auth.service';
import { PermissionService } from '../../service/permission.service';
import { ThemeService } from '../../services/theme.service';
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
  enrolledAt?: string;
  expiresAt?: string;
  tasks: DashboardTask[];
}

@Component({
  selector: 'app-dashboard',
  imports: [CommonModule, RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit {
  private authService = inject(AuthService);
  protected permissionService = inject(PermissionService);
  protected themeService = inject(ThemeService);

  protected courses = signal<DashboardCourse[]>([]);
  protected isLoading = signal(true);
  protected errorMessage = signal<string | null>(null);
  protected isAdmin = this.authService.isAdmin();
  protected isTeacher = this.authService.isTeacher();

  async ngOnInit(): Promise<void> {
    await this.loadCourses();
  }

  private async loadCourses(): Promise<void> {
    this.isLoading.set(true);
    try {
      const userId = this.authService.getUserId()();
      if (!userId) {
        this.errorMessage.set('Benutzer-ID nicht gefunden.');
        return;
      }

      const res = await apiClient.get<DashboardCourse[]>(
        `/api/student-courses/user/${userId}/dashboard`
      );
      this.courses.set(res.data ?? []);
    } catch {
      this.errorMessage.set('Kurse konnten nicht geladen werden.');
    } finally {
      this.isLoading.set(false);
    }
  }

  logout() {
    this.authService.logout();
  }

  toggleTheme() {
    this.themeService.toggleTheme();
  }
}
