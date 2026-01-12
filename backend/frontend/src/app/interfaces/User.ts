import { Instance } from './Instance';

export interface User {
  id: number;
  username: string;
  role: string;
  instances: Instance[];
}
