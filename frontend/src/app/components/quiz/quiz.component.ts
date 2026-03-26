import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { QuizService } from '../../services/quiz.service';
import { Quiz } from '../../models/quiz.model';

@Component({
  selector: 'app-quiz',
  imports: [CommonModule],
  templateUrl: './quiz.component.html',
  styleUrl: './quiz.component.css'
})
export class QuizComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private quizService = inject(QuizService);

  protected quiz?: Quiz;
  protected currentQuestionIndex = signal(0);
  protected selectedAnswers = signal<(number | null)[]>([]);
  protected showResults = signal(false);

  protected readonly String = String;

  protected currentQuestion = computed(() => {
    if (!this.quiz) return null;
    return this.quiz.questions[this.currentQuestionIndex()];
  });

  protected progress = computed(() => {
    if (!this.quiz) return 0;
    return ((this.currentQuestionIndex() + 1) / this.quiz.questions.length) * 100;
  });

  protected score = computed(() => {
    if (!this.quiz) return 0;
    let correct = 0;
    this.selectedAnswers().forEach((answer, index) => {
      if (answer === this.quiz!.questions[index].correctAnswer) correct++;
    });
    return correct;
  });

  ngOnInit(): void {
    const quizId = this.route.snapshot.paramMap.get('id');
    if (quizId) {
      this.quiz = this.quizService.getQuizById(quizId);
      if (this.quiz) {
        this.selectedAnswers.set(new Array(this.quiz.questions.length).fill(null));
      } else {
        this.router.navigate(['/exercises']);
      }
    }
  }

  selectAnswer(answerIndex: number): void {
    const answers = [...this.selectedAnswers()];
    answers[this.currentQuestionIndex()] = answerIndex;
    this.selectedAnswers.set(answers);
  }

  nextQuestion(): void {
    if (this.quiz && this.currentQuestionIndex() < this.quiz.questions.length - 1) {
      this.currentQuestionIndex.set(this.currentQuestionIndex() + 1);
    }
  }

  previousQuestion(): void {
    if (this.currentQuestionIndex() > 0) {
      this.currentQuestionIndex.set(this.currentQuestionIndex() - 1);
    }
  }

  finishQuiz(): void {
    this.showResults.set(true);
  }

  restartQuiz(): void {
    this.currentQuestionIndex.set(0);
    this.selectedAnswers.set(new Array(this.quiz!.questions.length).fill(null));
    this.showResults.set(false);
  }

  exitQuiz(): void {
    this.router.navigate(['/exercises']);
  }

  isAnswerCorrect(questionIndex: number): boolean {
    if (!this.quiz) return false;
    return this.selectedAnswers()[questionIndex] === this.quiz.questions[questionIndex].correctAnswer;
  }
}
