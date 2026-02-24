import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import apiClient from '../../service/api.service';

type Tab = 'courses' | 'images' | 'liveenvs' | 'users' | 'tasks' | 'enrollment';

interface CourseOption { id: number; name: string; }
interface ImageOption  { id: number; name: string; }
interface TaskOption   { id: number; title: string; }
interface UserOption   { id: number; name: string; className: string; }

@Component({
  selector: 'app-admin',
  imports: [CommonModule, FormsModule],
  templateUrl: './admin.html',
  styleUrl: './admin.css',
})
export class Admin implements OnInit {
  constructor(private router: Router) {}

  activeTab = signal<Tab>('courses');

  // Status
  successMsg = signal<string | null>(null);
  errorMsg   = signal<string | null>(null);
  loading    = signal(false);

  // Dropdown data
  courseOptions = signal<CourseOption[]>([]);
  imageOptions  = signal<ImageOption[]>([]);
  taskOptions   = signal<TaskOption[]>([]);
  userOptions   = signal<UserOption[]>([]);
  classOptions  = signal<string[]>([]);

  // Course form
  course = { name: '', description: '' };

  // Image form
  image = { name: '', imageRef: '' };

  // LiveEnvironment form
  liveEnv = { userId: '', vncPort: '', vncHost: '', vncPassword: '', status: 'active' };

  // User form
  user = { name: '', email: '', password: '', className: '', role: 'SCHUELER', expiredAt: '' };

  // Task form
  task = { title: '', description: '', points: 0, imageId: '' };

  // Course-Task assignment form
  assignment = { courseId: '', taskId: '', orderIndex: 0 };

  // Enrollment forms
  enrollUser  = { userId: '', courseId: '', expiresAt: '' };
  enrollClass = { className: '', courseId: '', expiresAt: '' };
  enrollResult = signal<{ enrolled: number; skipped: number; message: string } | null>(null);

  async ngOnInit() {
    await this.loadDropdowns();
  }

  private async loadDropdowns() {
    try {
      const [coursesRes, imagesRes, tasksRes, usersRes] = await Promise.all([
        apiClient.get<CourseOption[]>('/api/courses'),
        apiClient.get<ImageOption[]>('/api/images'),
        apiClient.get<TaskOption[]>('/api/tasks'),
        apiClient.get<UserOption[]>('/api/users'),
      ]);
      this.courseOptions.set(coursesRes.data ?? []);
      this.imageOptions.set(imagesRes.data ?? []);
      this.taskOptions.set(tasksRes.data ?? []);
      this.userOptions.set(usersRes.data ?? []);
      // Eindeutige Klassen aus Users ableiten
      const classes = [...new Set(
        (usersRes.data ?? []).map(u => u.className).filter(Boolean)
      )].sort();
      this.classOptions.set(classes);
    } catch {
      // dropdowns bleiben leer – manuelle Eingabe möglich
    }
  }

  setTab(tab: Tab) {
    this.activeTab.set(tab);
    this.clearMessages();
    this.enrollResult.set(null);
  }

  goBack() {
    this.router.navigate(['/dashboard']);
  }

  private clearMessages() {
    this.successMsg.set(null);
    this.errorMsg.set(null);
  }

  private handleSuccess(msg: string) {
    this.successMsg.set(msg);
    this.errorMsg.set(null);
    this.loading.set(false);
  }

  private handleError(err: unknown) {
    const msg = (err as { response?: { data?: { error?: string } } })?.response?.data?.error
      ?? (err instanceof Error ? err.message : 'Unbekannter Fehler');
    this.errorMsg.set(msg);
    this.successMsg.set(null);
    this.loading.set(false);
  }

  async createCourse() {
    this.loading.set(true);
    this.clearMessages();
    try {
      await apiClient.post('/api/courses', this.course);
      this.handleSuccess(`Kurs "${this.course.name}" erfolgreich erstellt.`);
      this.course = { name: '', description: '' };
      await this.loadDropdowns();
    } catch (e) { this.handleError(e); }
  }

  async createImage() {
    this.loading.set(true);
    this.clearMessages();
    try {
      await apiClient.post('/api/images', this.image);
      this.handleSuccess(`Image "${this.image.name}" erfolgreich erstellt.`);
      this.image = { name: '', imageRef: '' };
      await this.loadDropdowns();
    } catch (e) { this.handleError(e); }
  }

  async createLiveEnv() {
    this.loading.set(true);
    this.clearMessages();
    try {
      const payload = {
        userId: Number(this.liveEnv.userId),
        vncPort: Number(this.liveEnv.vncPort),
        vncHost: this.liveEnv.vncHost,
        vncPassword: this.liveEnv.vncPassword,
        status: this.liveEnv.status,
      };
      await apiClient.post('/api/live-environments', payload);
      this.handleSuccess(`Live-Umgebung für User ${this.liveEnv.userId} erfolgreich erstellt.`);
      this.liveEnv = { userId: '', vncPort: '', vncHost: '', vncPassword: '', status: 'active' };
    } catch (e) { this.handleError(e); }
  }

  async createUser() {
    this.loading.set(true);
    this.clearMessages();
    try {
      const payload = {
        name: this.user.name,
        email: this.user.email,
        password: this.user.password,
        className: this.user.className,
        role: this.user.role,
        expiredAt: this.user.expiredAt ? new Date(this.user.expiredAt).toISOString() : null,
      };
      await apiClient.post('/api/users', payload);
      this.handleSuccess(`Benutzer "${this.user.name}" erfolgreich erstellt.`);
      this.user = { name: '', email: '', password: '', className: '', role: 'SCHUELER', expiredAt: '' };
    } catch (e) { this.handleError(e); }
  }

  async createTask() {
    this.loading.set(true);
    this.clearMessages();
    try {
      const payload = {
        title: this.task.title,
        description: this.task.description,
        points: Number(this.task.points),
        imageId: Number(this.task.imageId),
      };
      const res = await apiClient.post<TaskOption>('/api/tasks', payload);
      this.handleSuccess(`Aufgabe "${this.task.title}" erfolgreich erstellt.`);
      this.task = { title: '', description: '', points: 0, imageId: '' };
      await this.loadDropdowns();
      // Neue Task direkt im Zuordnungs-Dropdown vorauswählen
      if (res.data?.id) {
        this.assignment.taskId = String(res.data.id);
      }
    } catch (e) { this.handleError(e); }
  }

  async assignTaskToCourse() {
    this.loading.set(true);
    this.clearMessages();
    try {
      const payload = {
        courseId: Number(this.assignment.courseId),
        taskId:   Number(this.assignment.taskId),
        orderIndex: Number(this.assignment.orderIndex),
      };
      await apiClient.post('/api/course-tasks', payload);
      const courseName = this.courseOptions().find(c => c.id === payload.courseId)?.name ?? payload.courseId;
      const taskTitle  = this.taskOptions().find(t => t.id === payload.taskId)?.title ?? payload.taskId;
      this.handleSuccess(`Aufgabe "${taskTitle}" wurde Kurs "${courseName}" zugeordnet.`);
      this.assignment = { courseId: '', taskId: '', orderIndex: 0 };
    } catch (e) { this.handleError(e); }
  }

  countUsersInClass(className: string): number {
    return this.userOptions().filter(u => u.className === className).length;
  }

  async enrollSingleUser() {
    this.loading.set(true);
    this.clearMessages();
    this.enrollResult.set(null);
    try {
      const payload: Record<string, unknown> = {
        userId:   Number(this.enrollUser.userId),
        courseId: Number(this.enrollUser.courseId),
      };
      if (this.enrollUser.expiresAt) {
        payload['expiresAt'] = new Date(this.enrollUser.expiresAt).toISOString().slice(0, 19);
      }
      await apiClient.post('/api/student-courses', payload);
      const userName   = this.userOptions().find(u => u.id === payload['userId'])?.name ?? payload['userId'];
      const courseName = this.courseOptions().find(c => c.id === payload['courseId'])?.name ?? payload['courseId'];
      this.handleSuccess(`Benutzer "${userName}" wurde in Kurs "${courseName}" eingeschrieben.`);
      this.enrollUser = { userId: '', courseId: '', expiresAt: '' };
    } catch (e) { this.handleError(e); }
  }

  async enrollByClass() {
    this.loading.set(true);
    this.clearMessages();
    this.enrollResult.set(null);
    try {
      const payload: Record<string, unknown> = {
        courseId:  Number(this.enrollClass.courseId),
        className: this.enrollClass.className,
      };
      if (this.enrollClass.expiresAt) {
        payload['expiresAt'] = new Date(this.enrollClass.expiresAt).toISOString().slice(0, 19);
      }
      const res = await apiClient.post<{ enrolled: number; skipped: number; message: string }>(
        '/api/student-courses/enroll-class', payload
      );
      this.enrollResult.set(res.data);
      this.handleSuccess(res.data.message);
      this.enrollClass = { className: '', courseId: '', expiresAt: '' };
    } catch (e) { this.handleError(e); }
  }
}
