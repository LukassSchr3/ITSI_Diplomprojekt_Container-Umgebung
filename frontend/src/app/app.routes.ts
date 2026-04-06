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
import { TaskDetail } from './components/task-detail/task-detail';
import { Admin } from './components/admin/admin';
import { authGuard } from './guards/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'dashboard', component: DashboardComponent, canActivate: [authGuard] },
  { path: 'task/:id', component: TaskDetail, canActivate: [authGuard] },
  { path: 'exercises', component: ExerciseListComponent, canActivate: [authGuard] },
  { path: 'exercises/:id', component: ExerciseDetailComponent, canActivate: [authGuard] },
  { path: 'image/:id', component: ImageComponent, canActivate: [authGuard] },
  { path: 'quiz-start/:exerciseId', component: QuizStartComponent, canActivate: [authGuard] },
  { path: 'quiz/:id', component: QuizComponent, canActivate: [authGuard] },
  { path: 'admin', component: Admin, canActivate: [authGuard] },
  { path: 'admin/quiz', component: AdminQuizComponent, canActivate: [authGuard] },
  { path: 'admin/exercises', component: AdminExercisesComponent, canActivate: [authGuard] },
];
