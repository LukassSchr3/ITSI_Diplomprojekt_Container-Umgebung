import { Injectable, computed, inject } from '@angular/core';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class PermissionService {
  private auth = inject(AuthService);

  canRead = computed(() => this.auth.isLoggedIn()());

  canWrite = computed(() => this.auth.isAdmin()());

  canGrade = computed(() => this.auth.isTeacher()() || this.auth.isAdmin()());

  canManageContainer(ownerId: string | number | null | undefined): boolean {
    const userId = this.auth.getUserId()();
    if (this.auth.isAdmin()()) return true;
    if (!userId || !ownerId) return false;
    return String(userId) === String(ownerId);
  }
}
