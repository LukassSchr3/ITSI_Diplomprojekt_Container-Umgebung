/**
 * Frontend Permission System - TypeScript/JavaScript Implementation
 *
 * Dieses File kann direkt ins Frontend-Projekt kopiert werden.
 * Funktioniert mit React, Vue, Angular, oder Vanilla JS.
 */

// ==================== Types ====================

export type Role = 'SCHUELER' | 'LEHRER' | 'ADMIN';
export type Action = 'read' | 'write' | 'grade' | 'admin';

export interface User {
  id: number;
  name: string;
  email: string;
  password?: string | null;
  className?: string;
  role: Role;
  createdAt?: string;
  expiredAt?: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  success: boolean;
  message: string;
  token: string | null;
  user: User | null;
}

// ==================== API Configuration ====================

const API_BASE_URL = '/api'; // oder 'http://localhost:9090/api'

// ==================== Storage Helper ====================

export class AuthStorage {
  private static TOKEN_KEY = 'jwt_token';
  private static USER_KEY = 'user_data';

  static saveAuth(token: string, user: User): void {
    localStorage.setItem(this.TOKEN_KEY, token);
    localStorage.setItem(this.USER_KEY, JSON.stringify(user));
  }

  static getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  static getUser(): User | null {
    const userData = localStorage.getItem(this.USER_KEY);
    return userData ? JSON.parse(userData) : null;
  }

  static clearAuth(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
  }

  static isAuthenticated(): boolean {
    return this.getToken() !== null && this.getUser() !== null;
  }
}

// ==================== Permission Logic ====================

export class PermissionService {
  /**
   * Prüft, ob ein User eine bestimmte Aktion ausführen darf
   */
  static canPerformAction(user: User | null, action: Action): boolean {
    if (!user) return false;

    // Admin kann alles
    if (user.role === 'ADMIN') return true;

    // Check nach Aktion
    switch (action) {
      case 'read':
        // Alle authentifizierten User können lesen
        return true;

      case 'write':
      case 'grade':
        // Nur LEHRER und ADMIN können schreiben/bewerten
        return user.role === 'LEHRER' || user.role === 'ADMIN';

      case 'admin':
        // Nur ADMIN
        return user.role === 'ADMIN';

      default:
        return false;
    }
  }

  /**
   * Helper-Funktionen für häufige Checks
   */
  static canRead(user: User | null): boolean {
    return this.canPerformAction(user, 'read');
  }

  static canWrite(user: User | null): boolean {
    return this.canPerformAction(user, 'write');
  }

  static canGrade(user: User | null): boolean {
    return this.canPerformAction(user, 'grade');
  }

  static isAdmin(user: User | null): boolean {
    return this.canPerformAction(user, 'admin');
  }

  /**
   * Prüft, ob ein Account abgelaufen ist
   */
  static isAccountExpired(user: User): boolean {
    if (!user.expiredAt) return false;
    const expiredDate = new Date(user.expiredAt);
    const now = new Date();
    return expiredDate < now;
  }
}

// ==================== API Service ====================

export class ApiService {
  /**
   * Generischer API-Call mit automatischem JWT-Token
   */
  static async call<T>(
    endpoint: string,
    options: RequestInit = {}
  ): Promise<T> {
    const token = AuthStorage.getToken();

    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
      ...options,
      headers: {
        'Content-Type': 'application/json',
        ...(token && { Authorization: `Bearer ${token}` }),
        ...options.headers,
      },
    });

    // Handle 401/403 - Session abgelaufen oder keine Berechtigung
    if (response.status === 401 || response.status === 403) {
      AuthStorage.clearAuth();
      window.location.href = '/login';
      throw new Error('Unauthorized');
    }

    // Parse Response
    const data = await response.json();

    if (!response.ok) {
      throw new Error(data.message || 'API Error');
    }

    return data as T;
  }

  /**
   * Login
   */
  static async login(email: string, password: string): Promise<User> {
    const response = await this.call<LoginResponse>('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    });

    if (response.success && response.token && response.user) {
      AuthStorage.saveAuth(response.token, response.user);
      return response.user;
    } else {
      throw new Error(response.message || 'Login failed');
    }
  }

  /**
   * Logout
   */
  static logout(): void {
    AuthStorage.clearAuth();
    window.location.href = '/login';
  }
}

// ==================== React Hook (Optional) ====================

/**
 * React Hook für Permissions
 *
 * Usage:
 * const { user, canGrade, canWrite, isAdmin } = usePermissions();
 */
export function usePermissions() {
  const user = AuthStorage.getUser();

  return {
    user,
    isAuthenticated: AuthStorage.isAuthenticated(),
    canRead: PermissionService.canRead(user),
    canWrite: PermissionService.canWrite(user),
    canGrade: PermissionService.canGrade(user),
    isAdmin: PermissionService.isAdmin(user),
    isExpired: user ? PermissionService.isAccountExpired(user) : false,
  };
}

// ==================== Examples ====================

/**
 * Example 1: Login
 */
async function exampleLogin() {
  try {
    const user = await ApiService.login('lehrer@test.com', 'password123');
    console.log('Login successful:', user);
    console.log('Can grade?', PermissionService.canGrade(user));
  } catch (error) {
    console.error('Login failed:', error);
  }
}

/**
 * Example 2: Check Permission
 */
function examplePermissionCheck() {
  const user = AuthStorage.getUser();

  if (PermissionService.canGrade(user)) {
    console.log('User can grade tasks');
  } else {
    console.log('User cannot grade tasks');
  }
}

/**
 * Example 3: Protected API Call
 */
async function exampleProtectedApiCall() {
  try {
    const tasks = await ApiService.call('/tasks', { method: 'GET' });
    console.log('Tasks:', tasks);
  } catch (error) {
    console.error('Failed to fetch tasks:', error);
  }
}

/**
 * Example 4: React Component
 */
// function MyComponent() {
//   const { canGrade, canWrite, isAdmin } = usePermissions();
//
//   return (
//     <div>
//       {canWrite && <button>Neue Aufgabe</button>}
//       {canGrade && <button>Bewerten</button>}
//       {isAdmin && <button>Admin Panel</button>}
//     </div>
//   );
// }

/**
 * Example 5: Vue Composition API
 */
// import { computed } from 'vue';
//
// export function usePermissions() {
//   const user = computed(() => AuthStorage.getUser());
//
//   return {
//     user,
//     canGrade: computed(() => PermissionService.canGrade(user.value)),
//     canWrite: computed(() => PermissionService.canWrite(user.value)),
//     isAdmin: computed(() => PermissionService.isAdmin(user.value)),
//   };
// }

/**
 * Example 6: Router Guard (React Router)
 */
// import { Navigate } from 'react-router-dom';
//
// function ProtectedRoute({ children, requiredAction }) {
//   const { user } = usePermissions();
//
//   if (!user) {
//     return <Navigate to="/login" />;
//   }
//
//   if (!PermissionService.canPerformAction(user, requiredAction)) {
//     return <Navigate to="/forbidden" />;
//   }
//
//   return children;
// }

/**
 * Example 7: Angular Guard
 */
// @Injectable()
// export class AuthGuard implements CanActivate {
//   canActivate(route: ActivatedRouteSnapshot): boolean {
//     const user = AuthStorage.getUser();
//     const requiredAction = route.data['requiredAction'];
//
//     if (!user) {
//       this.router.navigate(['/login']);
//       return false;
//     }
//
//     if (!PermissionService.canPerformAction(user, requiredAction)) {
//       this.router.navigate(['/forbidden']);
//       return false;
//     }
//
//     return true;
//   }
// }

