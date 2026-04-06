import { Component, OnInit, signal, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AdminService, CourseItem, ImageItem, LiveEnvItem, UserItem, TaskItem, QuestionItem } from '../../service/admin.service';
import { ThemeService } from '../../services/theme.service';

type Tab = 'courses' | 'images' | 'liveenvs' | 'users' | 'tasks' | 'enrollment' | 'questions';
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
  protected themeService = inject(ThemeService);
  private router = inject(Router);
  private adminService = inject(AdminService);

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

  // Full list data for overview tables
  courseList   = signal<CourseItem[]>([]);
  imageList    = signal<ImageItem[]>([]);
  liveEnvList  = signal<LiveEnvItem[]>([]);
  userList     = signal<UserItem[]>([]);
  taskList     = signal<TaskItem[]>([]);
  questionList = signal<QuestionItem[]>([]);

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

  // Question form
  question = { taskId: '', frage: '', antworten: '[]', bestehgrenzeProzent: 50, maximalpunkte: 10 };

  // Enrollment forms
  enrollUser  = { userId: '', courseId: '', expiresAt: '' };
  enrollClass = { className: '', courseId: '', expiresAt: '' };
  enrollResult = signal<{ enrolled: number; skipped: number; message: string } | null>(null);

  async ngOnInit() {
    await this.refreshData();
  }

  private async refreshData() {
    try {
      const data = await this.adminService.loadAll();
      this.courseOptions.set(data.courses.map(c => ({ id: c.id, name: c.name })));
      this.imageOptions.set(data.images.map(i => ({ id: i.id, name: i.name })));
      this.taskOptions.set(data.tasks.map(t => ({ id: t.id, title: t.title })));
      this.userOptions.set(data.users.map(u => ({ id: u.id, name: u.name, className: u.className ?? '' })));
      this.courseList.set(data.courses);
      this.imageList.set(data.images);
      this.taskList.set(data.tasks);
      this.userList.set(data.users);
      this.liveEnvList.set(data.liveEnvs);
      this.questionList.set(data.questions);
      const classes = [...new Set(data.users.map(u => u.className).filter(Boolean))].sort();
      this.classOptions.set(classes as string[]);
    } catch {
      // Dropdowns bleiben leer – manuelle Eingabe möglich
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
      await this.adminService.createCourse(this.course);
      this.handleSuccess(`Kurs "${this.course.name}" erfolgreich erstellt.`);
      this.course = { name: '', description: '' };
      await this.refreshData();
    } catch (e) { this.handleError(e); }
  }

  async createImage() {
    this.loading.set(true);
    this.clearMessages();
    try {
      await this.adminService.createImage(this.image);
      this.handleSuccess(`Image "${this.image.name}" erfolgreich erstellt.`);
      this.image = { name: '', imageRef: '' };
      await this.refreshData();
    } catch (e) { this.handleError(e); }
  }

  async createLiveEnv() {
    this.loading.set(true);
    this.clearMessages();
    try {
      await this.adminService.createLiveEnv({
        userId: Number(this.liveEnv.userId),
        vncPort: Number(this.liveEnv.vncPort),
        vncHost: this.liveEnv.vncHost,
        vncPassword: this.liveEnv.vncPassword,
        status: this.liveEnv.status,
      });
      this.handleSuccess(`Live-Umgebung für User ${this.liveEnv.userId} erfolgreich erstellt.`);
      this.liveEnv = { userId: '', vncPort: '', vncHost: '', vncPassword: '', status: 'active' };
    } catch (e) { this.handleError(e); }
  }

  async createUser() {
    this.loading.set(true);
    this.clearMessages();
    try {
      await this.adminService.createUser({
        name: this.user.name,
        email: this.user.email,
        password: this.user.password,
        className: this.user.className,
        role: this.user.role,
        expiredAt: this.user.expiredAt ? new Date(this.user.expiredAt).toISOString() : null,
      });
      this.handleSuccess(`Benutzer "${this.user.name}" erfolgreich erstellt.`);
      this.user = { name: '', email: '', password: '', className: '', role: 'SCHUELER', expiredAt: '' };
    } catch (e) { this.handleError(e); }
  }

  async createTask() {
    this.loading.set(true);
    this.clearMessages();
    try {
      const created = await this.adminService.createTask({
        title: this.task.title,
        description: this.task.description,
        points: Number(this.task.points),
        imageId: Number(this.task.imageId),
      });
      this.handleSuccess(`Aufgabe "${this.task.title}" erfolgreich erstellt.`);
      this.task = { title: '', description: '', points: 0, imageId: '' };
      await this.refreshData();
      if (created?.id) {
        this.assignment.taskId = String(created.id);
      }
    } catch (e) { this.handleError(e); }
  }

  async assignTaskToCourse() {
    this.loading.set(true);
    this.clearMessages();
    try {
      await this.adminService.assignTaskToCourse({
        courseId: Number(this.assignment.courseId),
        taskId:   Number(this.assignment.taskId),
        orderIndex: Number(this.assignment.orderIndex),
      });
      const courseName = this.courseOptions().find(c => c.id === Number(this.assignment.courseId))?.name ?? this.assignment.courseId;
      const taskTitle  = this.taskOptions().find(t => t.id === Number(this.assignment.taskId))?.title ?? this.assignment.taskId;
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
      await this.adminService.enrollSingleUser(payload);
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
      const result = await this.adminService.enrollByClass(payload);
      this.enrollResult.set(result);
      this.handleSuccess(result.message);
      this.enrollClass = { className: '', courseId: '', expiresAt: '' };
    } catch (e) { this.handleError(e); }
  }

  async deleteCourse(id: number) {
    if (!confirm('Kurs wirklich löschen?')) return;
    try {
      await this.adminService.deleteCourse(id);
      this.courseList.set(this.courseList().filter(c => c.id !== id));
      this.courseOptions.set(this.courseOptions().filter(c => c.id !== id));
      this.successMsg.set('Kurs gelöscht.');
    } catch (e) { this.handleError(e); }
  }

  async deleteImage(id: number) {
    if (!confirm('Image wirklich löschen?')) return;
    try {
      await this.adminService.deleteImage(id);
      this.imageList.set(this.imageList().filter(i => i.id !== id));
      this.imageOptions.set(this.imageOptions().filter(i => i.id !== id));
      this.successMsg.set('Image gelöscht.');
    } catch (e) { this.handleError(e); }
  }

  async deleteLiveEnv(id: number) {
    if (!confirm('Live-Umgebung wirklich löschen?')) return;
    try {
      await this.adminService.deleteLiveEnv(id);
      this.liveEnvList.set(this.liveEnvList().filter(e => e.id !== id));
      this.successMsg.set('Live-Umgebung gelöscht.');
    } catch (e) { this.handleError(e); }
  }

  async deleteUser(id: number) {
    if (!confirm('Benutzer wirklich löschen?')) return;
    try {
      await this.adminService.deleteUser(id);
      this.userList.set(this.userList().filter(u => u.id !== id));
      this.userOptions.set(this.userOptions().filter(u => u.id !== id));
      this.successMsg.set('Benutzer gelöscht.');
    } catch (e) { this.handleError(e); }
  }

  async deleteTask(id: number) {
    if (!confirm('Aufgabe wirklich löschen?')) return;
    try {
      await this.adminService.deleteTask(id);
      this.taskList.set(this.taskList().filter(t => t.id !== id));
      this.taskOptions.set(this.taskOptions().filter(t => t.id !== id));
      this.successMsg.set('Aufgabe gelöscht.');
    } catch (e) { this.handleError(e); }
  }

  async createQuestion() {
    this.loading.set(true);
    this.clearMessages();
    try {
      let antworten: unknown = this.question.antworten;
      try { antworten = JSON.parse(this.question.antworten); } catch { /* send as string */ }
      await this.adminService.createQuestion({
        taskId: Number(this.question.taskId),
        frage: this.question.frage,
        antworten,
        bestehgrenzeProzent: Number(this.question.bestehgrenzeProzent),
        maximalpunkte: Number(this.question.maximalpunkte),
      });
      this.handleSuccess('Frage erfolgreich erstellt.');
      this.question = { taskId: '', frage: '', antworten: '[]', bestehgrenzeProzent: 50, maximalpunkte: 10 };
      await this.refreshData();
    } catch (e) { this.handleError(e); }
  }

  async deleteQuestion(id: number) {
    if (!confirm('Frage wirklich löschen?')) return;
    try {
      await this.adminService.deleteQuestion(id);
      this.questionList.set(this.questionList().filter(q => q.id !== id));
      this.successMsg.set('Frage gelöscht.');
    } catch (e) { this.handleError(e); }
  }

  getTaskTitle(taskId: number): string {
    return this.taskOptions().find(t => t.id === taskId)?.title ?? String(taskId);
  }
}

