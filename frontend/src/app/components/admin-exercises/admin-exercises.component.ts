import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ExerciseService } from '../../services/exercise.service';
import { Exercise } from '../../models/exercise.model';

@Component({
  selector: 'app-admin-exercises',
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-exercises.component.html',
  styleUrl: './admin-exercises.component.css'
})
export class AdminExercisesComponent {
  private exerciseService = inject(ExerciseService);
  private router = inject(Router);

  protected exercises = this.exerciseService.getAllExercises();
  protected selectedExercise = signal<Exercise | null>(null);
  protected showForm = signal(false);
  protected isEditing = signal(false);

  protected exerciseForm = signal({
    id: '', title: '', description: '', category: ''
  });

  addNewExercise(): void {
    this.isEditing.set(false);
    this.showForm.set(true);
    this.exerciseForm.set({ id: '', title: '', description: '', category: '' });
  }

  editExercise(exercise: Exercise): void {
    this.isEditing.set(true);
    this.showForm.set(true);
    this.exerciseForm.set({
      id: exercise.id,
      title: exercise.title,
      description: exercise.description || '',
      category: exercise.category || ''
    });
  }

  saveExercise(): void {
    const form = this.exerciseForm();
    if (form.title && form.id) {
      if (this.isEditing()) {
        this.exerciseService.updateExercise(form.id, { title: form.title, description: form.description, category: form.category });
      } else {
        const newExercise: Exercise = {
          id: form.id, title: form.title, description: form.description,
          progress: 0, status: 'not-started', category: form.category
        };
        this.exerciseService.addExercise(newExercise);
      }
      this.showForm.set(false);
    }
  }

  deleteExercise(id: string): void {
    if (confirm('Übung wirklich löschen?')) {
      this.exerciseService.deleteExercise(id);
      this.selectedExercise.set(null);
    }
  }

  goBack(): void {
    this.router.navigate(['/dashboard']);
  }
}
