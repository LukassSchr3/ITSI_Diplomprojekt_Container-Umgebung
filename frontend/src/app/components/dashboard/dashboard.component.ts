import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ExerciseService } from '../../services/exercise.service';
import { AuthService } from '../../services/auth.service';
import { ThemeService } from '../../services/theme.service';
import { computed } from '@angular/core';

@Component({
  selector: 'app-dashboard',
  imports: [CommonModule, RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent {
  private exerciseService = inject(ExerciseService);
  private authService = inject(AuthService);
  protected themeService = inject(ThemeService);
  
  protected exercises = this.exerciseService.getExercises();
  protected role = this.authService.getRole();
  protected isTeacher = this.authService.isTeacher();

  protected stats = computed(() => {
    const all = this.exercises();
    const notStarted = all.filter(e => e.status === 'not-started').length;
    const inProgress = all.filter(e => e.status === 'in-progress').length;
    const completed = all.filter(e => e.status === 'completed').length;
    const total = all.length;

    return { notStarted, inProgress, completed, total };
  });

  logout() {
    this.authService.logout();
  }

  toggleTheme() {
    this.themeService.toggleTheme();
  }
}
