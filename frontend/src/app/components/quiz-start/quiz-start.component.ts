import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { QuizService } from '../../services/quiz.service';
import { Quiz } from '../../models/quiz.model';

@Component({
  selector: 'app-quiz-start',
  imports: [CommonModule, FormsModule],
  templateUrl: './quiz-start.component.html',
  styleUrl: './quiz-start.component.css'
})
export class QuizStartComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private quizService = inject(QuizService);

  protected quiz?: Quiz;
  protected password = '';
  protected errorMessage = '';
  protected isLoading = false;

  ngOnInit(): void {
    const exerciseId = this.route.snapshot.paramMap.get('exerciseId');
    if (exerciseId) {
      this.quiz = this.quizService.getQuizByExerciseId(exerciseId);
      if (!this.quiz) {
        this.errorMessage = 'Quiz nicht gefunden';
      }
    }
  }

  startQuiz(): void {
    if (!this.quiz) return;
    this.isLoading = true;
    this.errorMessage = '';
    setTimeout(() => {
      const isValid = this.quizService.verifyPassword(this.quiz!.id, this.password);
      if (isValid) {
        this.router.navigate(['/quiz', this.quiz!.id]);
      } else {
        this.errorMessage = 'Falsches Passwort';
        this.password = '';
      }
      this.isLoading = false;
    }, 300);
  }

  goBack(): void {
    this.router.navigate(['/exercises']);
  }
}
