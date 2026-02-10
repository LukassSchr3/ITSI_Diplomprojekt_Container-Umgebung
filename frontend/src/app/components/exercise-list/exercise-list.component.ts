import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { ExerciseService } from '../../service/exercise.service';
import { PermissionService } from '../../service/permission.service';
import { Exercise } from '../../models/exercise.model';

@Component({
  selector: 'app-exercise-list',
  imports: [CommonModule],
  templateUrl: './exercise-list.component.html',
  styleUrl: './exercise-list.component.css'
})
export class ExerciseListComponent {
  private exerciseService = inject(ExerciseService);
  private router = inject(Router);
  private permissions = inject(PermissionService);
  protected exercises = this.exerciseService.getExercises();
  protected canWrite = this.permissions.canWrite;

  onProgressChange(id: string, event: Event) {
    if (!this.canWrite()) return;
    const input = event.target as HTMLInputElement;
    const progress = parseInt(input.value, 10);
    this.exerciseService.updateProgress(id, progress);
  }

  onExerciseClick(exercise: Exercise) {
    console.log("Klick" + exercise.id);
    // Navigiere zum Image-Component mit der Exercise-ID
    // Die Exercise-ID (z.B. "9.1") wird als Image-ID verwendet
    this.router.navigate(['/image', exercise.id]);
  }
}
