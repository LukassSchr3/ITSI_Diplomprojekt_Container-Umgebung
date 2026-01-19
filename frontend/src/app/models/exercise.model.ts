export interface Exercise {
  id: string;
  title: string;
  description?: string;
  progress: number; // 0-100
  status: 'not-started' | 'in-progress' | 'completed';
  category?: string;
}
