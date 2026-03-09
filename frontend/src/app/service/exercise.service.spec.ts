import { TestBed } from '@angular/core/testing';

import { ExerciseService } from './exercise.service';

describe('ExerciseService', () => {
  let service: ExerciseService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ExerciseService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should return the default list of exercises', () => {
    const exercises = service.getExercises()();
    expect(exercises.length).toBe(3);
  });

  it('should have exercises with unique IDs', () => {
    const exercises = service.getExercises()();
    const ids = exercises.map((e) => e.id);
    const uniqueIds = [...new Set(ids)];
    expect(uniqueIds.length).toBe(ids.length);
  });

  it('should initialize all exercises with not-started status', () => {
    const exercises = service.getExercises()();
    exercises.forEach((e) => {
      expect(e.status).toBe('not-started');
      expect(e.progress).toBe(0);
    });
  });

  describe('updateProgress', () => {
    it('should set status to in-progress when progress is between 0 and 100', () => {
      const firstId = service.getExercises()()[0].id;
      service.updateProgress(firstId, 50);
      const exercise = service.getExercises()().find((e) => e.id === firstId)!;
      expect(exercise.progress).toBe(50);
      expect(exercise.status).toBe('in-progress');
    });

    it('should set status to completed when progress is 100', () => {
      const firstId = service.getExercises()()[0].id;
      service.updateProgress(firstId, 100);
      const exercise = service.getExercises()().find((e) => e.id === firstId)!;
      expect(exercise.progress).toBe(100);
      expect(exercise.status).toBe('completed');
    });

    it('should set status to not-started when progress is reset to 0', () => {
      const firstId = service.getExercises()()[0].id;
      service.updateProgress(firstId, 50);
      service.updateProgress(firstId, 0);
      const exercise = service.getExercises()().find((e) => e.id === firstId)!;
      expect(exercise.progress).toBe(0);
      expect(exercise.status).toBe('not-started');
    });

    it('should not affect other exercises', () => {
      const exercises = service.getExercises()();
      const firstId = exercises[0].id;
      service.updateProgress(firstId, 75);
      const others = service.getExercises()().filter((e) => e.id !== firstId);
      others.forEach((e) => {
        expect(e.progress).toBe(0);
        expect(e.status).toBe('not-started');
      });
    });
  });

  describe('updateStatus', () => {
    it('should set progress to 50 when status is in-progress', () => {
      const firstId = service.getExercises()()[0].id;
      service.updateStatus(firstId, 'in-progress');
      const exercise = service.getExercises()().find((e) => e.id === firstId)!;
      expect(exercise.status).toBe('in-progress');
      expect(exercise.progress).toBe(50);
    });

    it('should set progress to 100 when status is completed', () => {
      const firstId = service.getExercises()()[0].id;
      service.updateStatus(firstId, 'completed');
      const exercise = service.getExercises()().find((e) => e.id === firstId)!;
      expect(exercise.status).toBe('completed');
      expect(exercise.progress).toBe(100);
    });

    it('should set progress to 0 when status is not-started', () => {
      const firstId = service.getExercises()()[0].id;
      service.updateStatus(firstId, 'completed');
      service.updateStatus(firstId, 'not-started');
      const exercise = service.getExercises()().find((e) => e.id === firstId)!;
      expect(exercise.status).toBe('not-started');
      expect(exercise.progress).toBe(0);
    });
  });

  describe('markBewertet', () => {
    it('should mark an exercise as bewertet', () => {
      const firstId = service.getExercises()()[0].id;
      service.markBewertet(firstId);
      const exercise = service.getExercises()().find((e) => e.id === firstId)!;
      expect(exercise.bewertet).toBe(true);
    });

    it('should not change bewertet status of other exercises', () => {
      const exercises = service.getExercises()();
      const firstId = exercises[0].id;
      service.markBewertet(firstId);
      const others = service.getExercises()().filter((e) => e.id !== firstId);
      others.forEach((e) => {
        expect(e.bewertet).toBeFalsy();
      });
    });
  });
});
