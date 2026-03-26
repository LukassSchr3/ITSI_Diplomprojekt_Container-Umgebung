import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ExerciseService } from '../../services/exercise.service';
import { QuizService } from '../../services/quiz.service';
import { AuthService } from '../../services/auth.service';
import { Bewertung } from '../../models/exercise.model';

@Component({
  selector: 'app-exercise-list',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './exercise-list.component.html',
  styleUrl: './exercise-list.component.css'
})
export class ExerciseListComponent {
  private exerciseService = inject(ExerciseService);
  private quizService = inject(QuizService);
  private authService = inject(AuthService);
  private router = inject(Router);

  protected exercises = this.exerciseService.getExercises();
  protected isTeacher = this.authService.isTeacher();

  readonly bewertungOptions: Bewertung[] = [
    'Grundkompetenz nicht erfüllt',
    'Grundkompetenz überwiegend erfüllt',
    'Grundkompetenz vollständig erfüllt',
    'Erweiterte Kompetenz überwiegend erfüllt',
    'Erweiterte Kompetenz vollständig erfüllt'
  ];

  hasQuiz(exerciseId: string): boolean {
    return !!this.quizService.getQuizByExerciseId(exerciseId);
  }

  startQuiz(exerciseId: string): void {
    this.router.navigate(['/quiz-start', exerciseId]);
  }

  navigateTo(exerciseId: string): void {
    this.router.navigate(['/exercises', exerciseId]);
  }

  setBewertung(exerciseId: string, bewertung: string): void {
    if (this.isTeacher()) {
      this.exerciseService.setBewertung(exerciseId, bewertung as Bewertung || undefined);
    }
  }

  goBack() {
    this.router.navigate(['/dashboard']);
  }
}

