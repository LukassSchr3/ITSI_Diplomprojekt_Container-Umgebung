export type Bewertung =
  'Grundkompetenz nicht erfüllt' |
  'Grundkompetenz überwiegend erfüllt' |
  'Grundkompetenz vollständig erfüllt' |
  'Erweiterte Kompetenz überwiegend erfüllt' |
  'Erweiterte Kompetenz vollständig erfüllt';

export interface Exercise {
  id: string;
  title: string;
  description?: string;
  progress: number;
  status: 'not-started' | 'in-progress' | 'completed';
  category?: string;
  bewertung?: Bewertung;
}
