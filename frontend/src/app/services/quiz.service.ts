import { Injectable, signal } from '@angular/core';
import { Quiz, QuizQuestion } from '../models/quiz.model';

@Injectable({
  providedIn: 'root'
})
export class QuizService {
  private quizzes = signal<Quiz[]>(this.initializeQuizzes());

  private initializeQuizzes(): Quiz[] {
    return [
      {
        id: 'quiz-9.1',
        exerciseId: '9.1',
        title: 'ITSI 9.1 Netzwerkforensik Quiz',
        password: 'forensik123',
        questions: [
          { id: 'q1', question: 'Was ist Wireshark?', options: ['Ein Netzwerk-Analyse-Tool', 'Ein Antivirus-Programm', 'Ein Firewall-System', 'Ein VPN-Client'], correctAnswer: 0 },
          { id: 'q2', question: 'Welches Protokoll wird für DNS verwendet?', options: ['TCP Port 80', 'UDP Port 53', 'TCP Port 443', 'UDP Port 67'], correctAnswer: 1 },
          { id: 'q3', question: 'Was bedeutet die Abkürzung PCAP?', options: ['Protocol Capture', 'Packet Capture', 'Port Configuration', 'Protected Communication'], correctAnswer: 1 },
          { id: 'q4', question: 'Welcher Layer im OSI-Modell ist für IP-Adressen zuständig?', options: ['Layer 1', 'Layer 2', 'Layer 3', 'Layer 4'], correctAnswer: 2 }
        ]
      },
      {
        id: 'quiz-9.2',
        exerciseId: '9.2',
        title: 'ITSI 9.2 Memory Forensics Quiz',
        password: 'memory123',
        questions: [
          { id: 'q1', question: 'Was ist Volatility?', options: ['Ein Memory-Forensik-Framework', 'Ein Disk-Image-Tool', 'Ein Malware-Scanner', 'Ein Debugger'], correctAnswer: 0 },
          { id: 'q2', question: 'Was ist ein Memory Dump?', options: ['Ein Backup der Festplatte', 'Eine Kopie des RAM-Inhalts', 'Ein Log-File', 'Ein Registry-Export'], correctAnswer: 1 },
          { id: 'q3', question: 'Welches Format wird häufig für Memory Dumps verwendet?', options: ['.txt', '.raw/.mem', '.exe', '.dll'], correctAnswer: 1 },
          { id: 'q4', question: 'Was kann man in einem Memory Dump NICHT finden?', options: ['Prozesse', 'Netzwerkverbindungen', 'Gelöschte Dateien von der Festplatte', 'Passwörter im RAM'], correctAnswer: 2 }
        ]
      },
      {
        id: 'quiz-9.3',
        exerciseId: '9.3',
        title: 'ITSI 9.3 Malware-Analyse (Android) Quiz',
        password: 'android123',
        questions: [
          { id: 'q1', question: 'Was ist eine APK-Datei?', options: ['Android Package Kit', 'Apple Package Kit', 'Application Program Key', 'Advanced Protection Kit'], correctAnswer: 0 },
          { id: 'q2', question: 'Welches Tool wird für Android Reverse Engineering verwendet?', options: ['Wireshark', 'APKTool', 'Nmap', 'Metasploit'], correctAnswer: 1 },
          { id: 'q3', question: 'In welcher Sprache wird Android-App-Code primär geschrieben?', options: ['Python', 'C++', 'Java/Kotlin', 'JavaScript'], correctAnswer: 2 },
          { id: 'q4', question: 'Was ist der Android Debug Bridge (ADB)?', options: ['Ein Debugging-Tool', 'Ein Emulator', 'Ein Compiler', 'Ein Package Manager'], correctAnswer: 0 },
          { id: 'q5', question: 'Was bedeutet DEX in Android?', options: ['Data Execution', 'Dalvik Executable', 'Debug Extension', 'Device Exchange'], correctAnswer: 1 }
        ]
      }
    ];
  }

  getQuizByExerciseId(exerciseId: string): Quiz | undefined {
    return this.quizzes().find(quiz => quiz.exerciseId === exerciseId);
  }

  verifyPassword(quizId: string, password: string): boolean {
    const quiz = this.quizzes().find(q => q.id === quizId);
    return quiz?.password === password;
  }

  getQuizById(quizId: string): Quiz | undefined {
    return this.quizzes().find(q => q.id === quizId);
  }

  getAllQuizzes() {
    return this.quizzes.asReadonly();
  }

  addQuiz(quiz: Quiz): void {
    this.quizzes.update(quizzes => [...quizzes, quiz]);
  }

  updateQuiz(quizId: string, updatedQuiz: Partial<Quiz>): void {
    this.quizzes.update(quizzes =>
      quizzes.map(q => q.id === quizId ? { ...q, ...updatedQuiz } : q)
    );
  }

  deleteQuiz(quizId: string): void {
    this.quizzes.update(quizzes => quizzes.filter(q => q.id !== quizId));
  }

  addQuestion(quizId: string, question: QuizQuestion): void {
    this.quizzes.update(quizzes =>
      quizzes.map(q => q.id === quizId ? { ...q, questions: [...q.questions, question] } : q)
    );
  }

  updateQuestion(quizId: string, questionId: string, updatedQuestion: Partial<QuizQuestion>): void {
    this.quizzes.update(quizzes =>
      quizzes.map(q => q.id === quizId ? {
        ...q,
        questions: q.questions.map(question =>
          question.id === questionId ? { ...question, ...updatedQuestion } : question
        )
      } : q)
    );
  }

  deleteQuestion(quizId: string, questionId: string): void {
    this.quizzes.update(quizzes =>
      quizzes.map(q => q.id === quizId ? {
        ...q,
        questions: q.questions.filter(question => question.id !== questionId)
      } : q)
    );
  }
}
