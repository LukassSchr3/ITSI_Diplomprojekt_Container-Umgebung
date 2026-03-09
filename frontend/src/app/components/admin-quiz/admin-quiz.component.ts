import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { QuizService } from '../../services/quiz.service';
import { ExerciseService } from '../../services/exercise.service';
import { Quiz, QuizQuestion } from '../../models/quiz.model';

@Component({
  selector: 'app-admin-quiz',
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-quiz.component.html',
  styleUrl: './admin-quiz.component.css'
})
export class AdminQuizComponent {
  private quizService = inject(QuizService);
  private exerciseService = inject(ExerciseService);
  private router = inject(Router);

  protected quizzes = this.quizService.getAllQuizzes();
  protected exercises = this.exerciseService.getAllExercises();
  protected selectedQuiz = signal<Quiz | null>(null);
  protected editingQuestion = signal<QuizQuestion | null>(null);
  protected showQuestionForm = signal(false);
  protected showQuizForm = signal(false);

  protected readonly String = String;

  protected questionForm = signal({
    question: '', option1: '', option2: '', option3: '', option4: '', correctAnswer: 0
  });

  protected quizForm = signal({
    title: '', exerciseId: '', password: ''
  });

  selectQuiz(quiz: Quiz): void {
    this.selectedQuiz.set(quiz);
    this.showQuestionForm.set(false);
    this.editingQuestion.set(null);
  }

  addNewQuiz(): void {
    this.showQuizForm.set(true);
    this.quizForm.set({ title: '', exerciseId: '', password: '' });
  }

  saveQuiz(): void {
    const form = this.quizForm();
    if (form.title && form.exerciseId && form.password) {
      const newQuiz: Quiz = {
        id: 'quiz-' + Date.now(),
        title: form.title,
        exerciseId: form.exerciseId,
        password: form.password,
        questions: []
      };
      this.quizService.addQuiz(newQuiz);
      this.showQuizForm.set(false);
      this.selectedQuiz.set(newQuiz);
    }
  }

  deleteQuiz(quizId: string): void {
    if (confirm('Quiz wirklich löschen?')) {
      this.quizService.deleteQuiz(quizId);
      this.selectedQuiz.set(null);
    }
  }

  addQuestion(): void {
    this.showQuestionForm.set(true);
    this.editingQuestion.set(null);
    this.questionForm.set({ question: '', option1: '', option2: '', option3: '', option4: '', correctAnswer: 0 });
  }

  editQuestion(question: QuizQuestion): void {
    this.editingQuestion.set(question);
    this.showQuestionForm.set(true);
    this.questionForm.set({
      question: question.question,
      option1: question.options[0] || '',
      option2: question.options[1] || '',
      option3: question.options[2] || '',
      option4: question.options[3] || '',
      correctAnswer: question.correctAnswer
    });
  }

  saveQuestion(): void {
    const form = this.questionForm();
    const quiz = this.selectedQuiz();
    if (!quiz) return;
    const options = [form.option1, form.option2, form.option3, form.option4].filter(o => o);
    if (form.question && options.length >= 2) {
      const editing = this.editingQuestion();
      if (editing) {
        this.quizService.updateQuestion(quiz.id, editing.id, { question: form.question, options, correctAnswer: form.correctAnswer });
      } else {
        const newQuestion: QuizQuestion = { id: 'q' + Date.now(), question: form.question, options, correctAnswer: form.correctAnswer };
        this.quizService.addQuestion(quiz.id, newQuestion);
      }
      this.showQuestionForm.set(false);
      this.editingQuestion.set(null);
    }
  }

  deleteQuestion(questionId: string): void {
    const quiz = this.selectedQuiz();
    if (!quiz) return;
    if (confirm('Frage wirklich löschen?')) {
      this.quizService.deleteQuestion(quiz.id, questionId);
    }
  }

  goBack(): void {
    this.router.navigate(['/dashboard']);
  }
}
