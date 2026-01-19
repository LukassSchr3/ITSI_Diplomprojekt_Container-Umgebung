export interface Exercise {
  id: string;
  title: string;
  description?: string;
  progress: number;
  status: 'not-started' | 'in-progress' | 'completed';
  category?: string;
  imageId?: string; // ID des zugehörigen Docker-Images

  // true = vom Lehrer bewertet, false/undefined = noch nicht bewertet
  bewertet?: boolean;
}
