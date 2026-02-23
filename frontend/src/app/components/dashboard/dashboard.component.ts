import { Component, inject, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { ExerciseService } from '../../service/exercise.service';
import { AuthService } from '../../service/auth.service';
import { PermissionService } from '../../service/permission.service';
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
  private permissions = inject(PermissionService);

  protected exercises = this.exerciseService.getExercises();
  protected roles = this.authService.getRoles();
  protected canGrade = this.permissions.canGrade;
  protected canWrite = this.permissions.canWrite;

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

  onStatusChange(id: string, newStatus: 'not-started' | 'in-progress' | 'completed') {
    if (!this.canWrite()) return;
    this.exerciseService.updateStatus(id, newStatus);
  }

  onExerciseClick(exercise: Exercise) {
    console.log('Exercise clicked:', exercise.id, 'imageId:', exercise.imageId);
    // Navigiere zum Image-Component with der Image-ID
    if (exercise.imageId) {
      this.router.navigate(['/image', exercise.imageId]);
    } else {
      console.warn('No imageId defined for exercise:', exercise.id);
    }
  }

  onBewerten(id: string) {
    if (!this.canGrade()) return;
    this.exerciseService.markBewertet(id);
  }

  logout() {
    this.authService.logout();
  }
}
