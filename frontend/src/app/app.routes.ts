import { Routes } from '@angular/router';
import { DashboardComponent } from './components/dashboard/dashboard.component';
import { ImageComponent } from './components/image/imageComponent';
import { LoginComponent } from './components/login/login.component';
import { Admin } from './components/admin/admin';
import { TaskDetail } from './components/task-detail/task-detail';
import { authGuard } from './guards/auth.guard';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
  { path: 'dashboard', component: DashboardComponent, canActivate: [authGuard] },
  { path: 'task/:taskId', component: TaskDetail, canActivate: [authGuard] },
  { path: 'image/:id', component: ImageComponent, canActivate: [authGuard] },
  { path: 'admin', component: Admin, canActivate: [authGuard] },
];
