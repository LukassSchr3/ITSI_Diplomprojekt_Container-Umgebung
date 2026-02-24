export interface User {
  id: number;
  name: string;
  email: string;
  password?: string;
  className?: string;
  role?: string;
  createdAt?: string | null;
  expiredAt?: string | null;
}
