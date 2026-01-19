import { Component, inject, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { ExerciseService } from '../../service/exercise.service';
import { AuthService } from '../../service/auth.service';
import { Exercise } from '../../models/exercise.model';

@Component({
  selector: 'app-dashboard',
  imports: [CommonModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent {
  private exerciseService = inject(ExerciseService);
  private authService = inject(AuthService);
  private router = inject(Router);

  protected exercises = this.exerciseService.getExercises();

  protected stats = computed(() => {
    const all = this.exercises();
    const notStarted = all.filter(e => e.status === 'not-started').length;
    const inProgress = all.filter(e => e.status === 'in-progress').length;
    const completed = all.filter(e => e.status === 'completed').length;
    const total = all.length;

    return { notStarted, inProgress, completed, total };
  });

  onStatusChange(id: string, newStatus: 'not-started' | 'in-progress' | 'completed') {
    this.exerciseService.updateStatus(id, newStatus);
  }

  onExerciseClick(exercise: Exercise) {
    console.log('Exercise clicked:', exercise.id);
    // Navigiere zum Image-Component mit der Exercise-ID
    this.router.navigate(['/image', exercise.id]);
  }

  logout() {
    this.authService.logout();
  }
}
