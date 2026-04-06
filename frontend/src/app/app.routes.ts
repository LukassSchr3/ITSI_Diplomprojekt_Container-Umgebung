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
import { StudentProgressComponent } from './components/student-progress/student-progress.component';
import { TeacherProgressComponent } from './components/teacher-progress/teacher-progress.component';
import { authGuard } from './guards/auth.guard';
import { adminGuard } from './guards/admin.guard';

export const routes: Routes = [
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'dashboard', component: DashboardComponent, canActivate: [authGuard] },
  { path: 'progress', component: StudentProgressComponent, canActivate: [authGuard] },
  { path: 'teacher-progress', component: TeacherProgressComponent, canActivate: [authGuard] },
  { path: 'task/:id', component: TaskDetail, canActivate: [authGuard] },
  { path: 'exercises', component: ExerciseListComponent, canActivate: [authGuard] },
  { path: 'exercises/:id', component: ExerciseDetailComponent, canActivate: [authGuard] },
  { path: 'image/:id', component: ImageComponent, canActivate: [authGuard] },
  { path: 'quiz-start/:exerciseId', component: QuizStartComponent, canActivate: [authGuard] },
  { path: 'quiz/:id', component: QuizComponent, canActivate: [authGuard] },
  { path: 'admin', component: Admin, canActivate: [adminGuard] },
  { path: 'admin/quiz', component: AdminQuizComponent, canActivate: [adminGuard] },
  { path: 'admin/exercises', component: AdminExercisesComponent, canActivate: [adminGuard] },
];
