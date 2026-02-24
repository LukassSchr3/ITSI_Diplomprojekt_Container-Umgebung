export interface CourseTask {
  id: number;
  title: string;
  description?: string;
  points: number;
  imageId?: number;
}

export interface DashboardCourse {
  courseId: number;
  courseName: string;
  courseDescription?: string;
  enrolledAt: string;
  expiresAt?: string;
  tasks: CourseTask[];
}

