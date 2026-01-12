import { Routes } from '@angular/router';
import { Dashboard } from './dashboard/dashboard';
import { ImageComponent } from './image/imageComponent';

export const routes: Routes = [
  { path: '', component: Dashboard },
  { path: 'dashboard', component: Dashboard },
  { path: 'image/:id', component: ImageComponent },
];
