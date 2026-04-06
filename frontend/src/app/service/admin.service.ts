import { Injectable } from '@angular/core';
import apiClient from './api.service';

export interface CourseItem   { id: number; name: string; description?: string; }
export interface ImageItem    { id: number; name: string; imageRef: string; }
export interface LiveEnvItem  { id: number; userId: number; vncHost?: string; vncPort: number; status: string; }
export interface UserItem     { id: number; name: string; email: string; className?: string; role: string; }
export interface TaskItem     { id: number; title: string; description?: string; points?: number; imageId?: number; }
export interface QuestionItem { id: number; taskId: number; frage: string; antworten?: string; bestehgrenzeProzent?: number; maximalpunkte?: number; }

export interface AdminData {
  courses:   CourseItem[];
  images:    ImageItem[];
  tasks:     TaskItem[];
  users:     UserItem[];
  liveEnvs:  LiveEnvItem[];
  questions: QuestionItem[];
}

export interface EnrollResult { enrolled: number; skipped: number; message: string; }

@Injectable({ providedIn: 'root' })
export class AdminService {

  async loadAll(): Promise<AdminData> {
    const [coursesRes, imagesRes, tasksRes, usersRes, liveEnvsRes, questionsRes] = await Promise.all([
      apiClient.get<CourseItem[]>('/api/courses'),
      apiClient.get<ImageItem[]>('/api/images'),
      apiClient.get<TaskItem[]>('/api/tasks'),
      apiClient.get<UserItem[]>('/api/users'),
      apiClient.get<LiveEnvItem[]>('/api/live-environments'),
      apiClient.get<QuestionItem[]>('/api/questions'),
    ]);
    return {
      courses:   coursesRes.data   ?? [],
      images:    imagesRes.data    ?? [],
      tasks:     tasksRes.data     ?? [],
      users:     usersRes.data     ?? [],
      liveEnvs:  liveEnvsRes.data  ?? [],
      questions: questionsRes.data ?? [],
    };
  }

  // --- Courses ---
  async createCourse(data: { name: string; description: string }): Promise<CourseItem> {
    const res = await apiClient.post<CourseItem>('/api/courses', data);
    return res.data;
  }

  async deleteCourse(id: number): Promise<void> {
    await apiClient.delete(`/api/courses/${id}`);
  }

  // --- Images ---
  async createImage(data: { name: string; imageRef: string }): Promise<ImageItem> {
    const res = await apiClient.post<ImageItem>('/api/images', data);
    return res.data;
  }

  async deleteImage(id: number): Promise<void> {
    await apiClient.delete(`/api/images/${id}`);
  }

  // --- Live Environments ---
  async createLiveEnv(data: { userId: number; vncPort: number; vncHost: string; vncPassword: string; status: string }): Promise<LiveEnvItem> {
    const res = await apiClient.post<LiveEnvItem>('/api/live-environments', data);
    return res.data;
  }

  async deleteLiveEnv(id: number): Promise<void> {
    await apiClient.delete(`/api/live-environments/${id}`);
  }

  // --- Users ---
  async createUser(data: { name: string; email: string; password: string; className: string; role: string; expiredAt: string | null }): Promise<UserItem> {
    const res = await apiClient.post<UserItem>('/api/users', data);
    return res.data;
  }

  async deleteUser(id: number): Promise<void> {
    await apiClient.delete(`/api/users/${id}`);
  }

  // --- Tasks ---
  async createTask(data: { title: string; description: string; points: number; imageId: number }): Promise<TaskItem> {
    const res = await apiClient.post<TaskItem>('/api/tasks', data);
    return res.data;
  }

  async deleteTask(id: number): Promise<void> {
    await apiClient.delete(`/api/tasks/${id}`);
  }

  // --- Course-Task assignment ---
  async assignTaskToCourse(data: { courseId: number; taskId: number; orderIndex: number }): Promise<void> {
    await apiClient.post('/api/course-tasks', data);
  }

  // --- Questions ---
  async createQuestion(data: { taskId: number; frage: string; antworten: unknown; bestehgrenzeProzent: number; maximalpunkte: number }): Promise<QuestionItem> {
    const payload = {
      ...data,
      antworten: data.antworten ? JSON.stringify(data.antworten) : null
    };
    const res = await apiClient.post<QuestionItem>('/api/questions', payload);
    return res.data;
  }

  async deleteQuestion(id: number): Promise<void> {
    await apiClient.delete(`/api/questions/${id}`);
  }

  // --- Enrollment ---
  async enrollSingleUser(data: Record<string, unknown>): Promise<void> {
    await apiClient.post('/api/student-courses', data);
  }

  async enrollByClass(data: Record<string, unknown>): Promise<EnrollResult> {
    const res = await apiClient.post<EnrollResult>('/api/student-courses/enroll-class', data);
    return res.data;
  }
}
