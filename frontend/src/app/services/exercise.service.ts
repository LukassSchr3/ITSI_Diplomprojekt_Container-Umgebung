import { Injectable, signal } from '@angular/core';
import { Exercise, Bewertung } from '../models/exercise.model';

@Injectable({
  providedIn: 'root'
})
export class ExerciseService {
  private exercises = signal<Exercise[]>([
    {
      id: '9.1',
      title: 'ITSI 9.1 Netzwerkforensik',
      description: 'Network forensics and traffic analysis',
      progress: 0,
      status: 'not-started',
      category: 'Forensik'
    },
    {
      id: '9.2',
      title: 'ITSI 9.2 Memory Forensics',
      description: 'Memory dump analysis and investigation',
      progress: 0,
      status: 'not-started',
      category: 'Forensik'
    },
    {
      id: '9.3',
      title: 'ITSI 9.3 Malware-Analyse (Android)',
      description: 'Android malware analysis and reverse engineering',
      progress: 0,
      status: 'not-started',
      category: 'Malware'
    }
  ]);

  getExercises() {
    return this.exercises.asReadonly();
  }

  setBewertung(id: string, bewertung: Bewertung | undefined) {
    this.exercises.update(exercises =>
      exercises.map(ex => ex.id === id ? { ...ex, bewertung } : ex)
    );
  }

  updateProgress(id: string, progress: number) {
    this.exercises.update(exercises =>
      exercises.map(ex => {
        if (ex.id === id) {
          let status: Exercise['status'] = 'not-started';
          if (progress > 0 && progress < 100) status = 'in-progress';
          else if (progress === 100) status = 'completed';
          return { ...ex, progress, status };
        }
        return ex;
      })
    );
  }

  updateStatus(id: string, status: Exercise['status']) {
    this.exercises.update(exercises =>
      exercises.map(ex => {
        if (ex.id === id) {
          let progress = 0;
          if (status === 'in-progress') progress = 50;
          else if (status === 'completed') progress = 100;
          return { ...ex, status, progress };
        }
        return ex;
      })
    );
  }

  getAllExercises() {
    return this.exercises.asReadonly();
  }

  addExercise(exercise: Exercise): void {
    this.exercises.update(exercises => [...exercises, exercise]);
  }

  updateExercise(id: string, updatedExercise: Partial<Exercise>): void {
    this.exercises.update(exercises =>
      exercises.map(ex => ex.id === id ? { ...ex, ...updatedExercise } : ex)
    );
  }

  deleteExercise(id: string): void {
    this.exercises.update(exercises => exercises.filter(ex => ex.id !== id));
  }
}

