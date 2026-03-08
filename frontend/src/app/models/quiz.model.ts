export interface QuizQuestion {
  id: string;
  question: string;
  options: string[];
  correctAnswer: number; // Index der richtigen Antwort
}

export interface Quiz {
  id: string;
  exerciseId: string;
  title: string;
  password: string;
  questions: QuizQuestion[];
}

export interface QuizResult {
  quizId: string;
  score: number;
  totalQuestions: number;
  completedAt: Date;
}
