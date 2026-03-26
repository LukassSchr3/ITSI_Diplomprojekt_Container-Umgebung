import { Routes } from '@angular/router';
import { DashboardComponent } from './components/dashboard/dashboard.component';
import { LoginComponent } from './components/login/login.component';
import { ExerciseListComponent } from './components/exercise-list/exercise-list.component';
import { ExerciseDetailComponent } from './components/exercise-detail/exercise-detail.component';
import { QuizStartComponent } from './components/quiz-start/quiz-start.component';
import { QuizComponent } from './components/quiz/quiz.component';
import { AdminQuizComponent } from './components/admin-quiz/admin-quiz.component';
import { AdminExercisesComponent } from './components/admin-exercises/admin-exercises.component';
import { ImageComponent } from './components/image/imageComponent';

export const routes: Routes = [
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'dashboard', component: DashboardComponent },
  { path: 'exercises', component: ExerciseListComponent },
  { path: 'exercises/:id', component: ExerciseDetailComponent },
  { path: 'image/:id', component: ImageComponent },
  { path: 'quiz-start/:exerciseId', component: QuizStartComponent },
  { path: 'quiz/:id', component: QuizComponent },
  { path: 'admin/quiz', component: AdminQuizComponent },
  { path: 'admin/exercises', component: AdminExercisesComponent }
];
