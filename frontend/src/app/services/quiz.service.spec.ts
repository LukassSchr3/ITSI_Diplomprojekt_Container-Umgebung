import { TestBed } from '@angular/core/testing';

import { QuizService } from './quiz.service';
import type { Quiz, QuizQuestion } from '../models/quiz.model';

describe('QuizService', () => {
  let service: QuizService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(QuizService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  // ── Initial data ───────────────────────────────────────────────────────────

  describe('initial quizzes', () => {
    it('should initialize with exactly 3 quizzes', () => {
      expect(service.getAllQuizzes()().length).toBe(3);
    });

    it('should have quiz-9.1 for exercise 9.1', () => {
      const quiz = service.getQuizByExerciseId('9.1');
      expect(quiz).toBeDefined();
      expect(quiz!.id).toBe('quiz-9.1');
    });

    it('should have quiz-9.2 for exercise 9.2', () => {
      const quiz = service.getQuizByExerciseId('9.2');
      expect(quiz).toBeDefined();
      expect(quiz!.id).toBe('quiz-9.2');
    });

    it('should have quiz-9.3 for exercise 9.3', () => {
      const quiz = service.getQuizByExerciseId('9.3');
      expect(quiz).toBeDefined();
      expect(quiz!.id).toBe('quiz-9.3');
    });

    it('should have correct passwords for each quiz', () => {
      expect(service.getQuizById('quiz-9.1')!.password).toBe('forensik123');
      expect(service.getQuizById('quiz-9.2')!.password).toBe('memory123');
      expect(service.getQuizById('quiz-9.3')!.password).toBe('android123');
    });

    it('should have questions with correctAnswer index', () => {
      const quiz = service.getQuizById('quiz-9.1')!;
      quiz.questions.forEach((q) => {
        expect(typeof q.correctAnswer).toBe('number');
        expect(q.correctAnswer).toBeGreaterThanOrEqual(0);
        expect(q.correctAnswer).toBeLessThan(q.options.length);
      });
    });

    it('quiz-9.3 should have 5 questions', () => {
      expect(service.getQuizById('quiz-9.3')!.questions.length).toBe(5);
    });
  });

  // ── getQuizByExerciseId ────────────────────────────────────────────────────

  describe('getQuizByExerciseId', () => {
    it('should return undefined for an unknown exercise ID', () => {
      expect(service.getQuizByExerciseId('99.9')).toBeUndefined();
    });

    it('should return the correct quiz for a known exercise ID', () => {
      const quiz = service.getQuizByExerciseId('9.2');
      expect(quiz!.title).toContain('Memory Forensics');
    });
  });

  // ── getQuizById ────────────────────────────────────────────────────────────

  describe('getQuizById', () => {
    it('should return the quiz for a known quiz ID', () => {
      const quiz = service.getQuizById('quiz-9.1');
      expect(quiz).toBeDefined();
      expect(quiz!.exerciseId).toBe('9.1');
    });

    it('should return undefined for an unknown quiz ID', () => {
      expect(service.getQuizById('quiz-unknown')).toBeUndefined();
    });
  });

  // ── verifyPassword ─────────────────────────────────────────────────────────

  describe('verifyPassword', () => {
    it('should return true for a correct password', () => {
      expect(service.verifyPassword('quiz-9.1', 'forensik123')).toBe(true);
    });

    it('should return false for a wrong password', () => {
      expect(service.verifyPassword('quiz-9.1', 'wrongpassword')).toBe(false);
    });

    it('should return false for an empty string password', () => {
      expect(service.verifyPassword('quiz-9.1', '')).toBe(false);
    });

    it('should return false for a non-existent quiz ID', () => {
      expect(service.verifyPassword('quiz-does-not-exist', 'forensik123')).toBe(false);
    });

    it('should be case-sensitive', () => {
      expect(service.verifyPassword('quiz-9.1', 'Forensik123')).toBe(false);
      expect(service.verifyPassword('quiz-9.1', 'FORENSIK123')).toBe(false);
    });
  });

  // ── addQuiz ────────────────────────────────────────────────────────────────

  describe('addQuiz', () => {
    it('should increase the quiz count by 1', () => {
      const newQuiz: Quiz = {
        id: 'quiz-new',
        exerciseId: '10.1',
        title: 'New Quiz',
        password: 'newpass',
        questions: [],
      };
      service.addQuiz(newQuiz);
      expect(service.getAllQuizzes()().length).toBe(4);
    });

    it('should make the new quiz retrievable by ID', () => {
      const newQuiz: Quiz = {
        id: 'quiz-custom',
        exerciseId: '10.2',
        title: 'Custom Quiz',
        password: 'custom123',
        questions: [],
      };
      service.addQuiz(newQuiz);
      expect(service.getQuizById('quiz-custom')).toEqual(newQuiz);
    });

    it('should make the new quiz retrievable by exercise ID', () => {
      const newQuiz: Quiz = {
        id: 'quiz-ex10',
        exerciseId: '10.3',
        title: 'Exercise 10.3 Quiz',
        password: 'pass',
        questions: [],
      };
      service.addQuiz(newQuiz);
      expect(service.getQuizByExerciseId('10.3')).toEqual(newQuiz);
    });
  });

  // ── updateQuiz ─────────────────────────────────────────────────────────────

  describe('updateQuiz', () => {
    it('should update the title of an existing quiz', () => {
      service.updateQuiz('quiz-9.1', { title: 'Updated Title' });
      expect(service.getQuizById('quiz-9.1')!.title).toBe('Updated Title');
    });

    it('should update the password of an existing quiz', () => {
      service.updateQuiz('quiz-9.2', { password: 'newpassword' });
      expect(service.getQuizById('quiz-9.2')!.password).toBe('newpassword');
      expect(service.verifyPassword('quiz-9.2', 'newpassword')).toBe(true);
      expect(service.verifyPassword('quiz-9.2', 'memory123')).toBe(false);
    });

    it('should not affect other quizzes', () => {
      service.updateQuiz('quiz-9.1', { title: 'Changed' });
      const q2 = service.getQuizById('quiz-9.2')!;
      const q3 = service.getQuizById('quiz-9.3')!;
      expect(q2.title).toContain('Memory');
      expect(q3.title).toContain('Malware');
    });

    it('should not change anything for an unknown quiz ID', () => {
      const before = service.getAllQuizzes()().map((q) => q.title);
      service.updateQuiz('unknown-id', { title: 'Ghost' });
      const after = service.getAllQuizzes()().map((q) => q.title);
      expect(after).toEqual(before);
    });
  });

  // ── deleteQuiz ─────────────────────────────────────────────────────────────

  describe('deleteQuiz', () => {
    it('should reduce quiz count by 1', () => {
      service.deleteQuiz('quiz-9.1');
      expect(service.getAllQuizzes()().length).toBe(2);
    });

    it('should make the deleted quiz unretrievable', () => {
      service.deleteQuiz('quiz-9.2');
      expect(service.getQuizById('quiz-9.2')).toBeUndefined();
      expect(service.getQuizByExerciseId('9.2')).toBeUndefined();
    });

    it('should not affect remaining quizzes', () => {
      service.deleteQuiz('quiz-9.1');
      expect(service.getQuizById('quiz-9.2')).toBeDefined();
      expect(service.getQuizById('quiz-9.3')).toBeDefined();
    });

    it('should do nothing for an unknown quiz ID', () => {
      service.deleteQuiz('nonexistent');
      expect(service.getAllQuizzes()().length).toBe(3);
    });
  });

  // ── addQuestion ────────────────────────────────────────────────────────────

  describe('addQuestion', () => {
    const newQ: QuizQuestion = {
      id: 'qNew',
      question: 'What is TCP?',
      options: ['Protocol', 'OS', 'Firewall', 'Browser'],
      correctAnswer: 0,
    };

    it('should add a question to the specified quiz', () => {
      const before = service.getQuizById('quiz-9.1')!.questions.length;
      service.addQuestion('quiz-9.1', newQ);
      expect(service.getQuizById('quiz-9.1')!.questions.length).toBe(before + 1);
    });

    it('should place the new question last', () => {
      service.addQuestion('quiz-9.1', newQ);
      const questions = service.getQuizById('quiz-9.1')!.questions;
      expect(questions[questions.length - 1]).toEqual(newQ);
    });

    it('should not change other quizzes', () => {
      const before9_2 = service.getQuizById('quiz-9.2')!.questions.length;
      service.addQuestion('quiz-9.1', newQ);
      expect(service.getQuizById('quiz-9.2')!.questions.length).toBe(before9_2);
    });
  });

  // ── updateQuestion ─────────────────────────────────────────────────────────

  describe('updateQuestion', () => {
    it('should update the question text', () => {
      const quizId = 'quiz-9.1';
      const questionId = service.getQuizById(quizId)!.questions[0].id;
      service.updateQuestion(quizId, questionId, { question: 'Updated question text?' });
      const updated = service.getQuizById(quizId)!.questions.find((q) => q.id === questionId)!;
      expect(updated.question).toBe('Updated question text?');
    });

    it('should update the correctAnswer index', () => {
      const quizId = 'quiz-9.2';
      const questionId = service.getQuizById(quizId)!.questions[0].id;
      service.updateQuestion(quizId, questionId, { correctAnswer: 3 });
      const updated = service.getQuizById(quizId)!.questions.find((q) => q.id === questionId)!;
      expect(updated.correctAnswer).toBe(3);
    });

    it('should not affect other questions in the same quiz', () => {
      const quizId = 'quiz-9.1';
      const questions = service.getQuizById(quizId)!.questions;
      const firstId = questions[0].id;
      const secondOriginalText = questions[1].question;
      service.updateQuestion(quizId, firstId, { question: 'Changed?' });
      expect(service.getQuizById(quizId)!.questions[1].question).toBe(secondOriginalText);
    });
  });

  // ── deleteQuestion ─────────────────────────────────────────────────────────

  describe('deleteQuestion', () => {
    it('should remove the specified question', () => {
      const quizId = 'quiz-9.1';
      const before = service.getQuizById(quizId)!.questions.length;
      const firstId = service.getQuizById(quizId)!.questions[0].id;
      service.deleteQuestion(quizId, firstId);
      expect(service.getQuizById(quizId)!.questions.length).toBe(before - 1);
    });

    it('should make the deleted question no longer present', () => {
      const quizId = 'quiz-9.1';
      const firstId = service.getQuizById(quizId)!.questions[0].id;
      service.deleteQuestion(quizId, firstId);
      const ids = service.getQuizById(quizId)!.questions.map((q) => q.id);
      expect(ids).not.toContain(firstId);
    });

    it('should not affect other quizzes', () => {
      const quizId = 'quiz-9.1';
      const before9_2 = service.getQuizById('quiz-9.2')!.questions.length;
      const firstId = service.getQuizById(quizId)!.questions[0].id;
      service.deleteQuestion(quizId, firstId);
      expect(service.getQuizById('quiz-9.2')!.questions.length).toBe(before9_2);
    });

    it('should do nothing for an unknown question ID', () => {
      const quizId = 'quiz-9.1';
      const before = service.getQuizById(quizId)!.questions.length;
      service.deleteQuestion(quizId, 'nonexistent-question');
      expect(service.getQuizById(quizId)!.questions.length).toBe(before);
    });
  });
});
