import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { LoginResponse, Role, UserProfile } from './models';

const TOKEN_KEY = 'itsn.token';
const USER_KEY = 'itsn.user';

/** Session utilisateur : jeton JWT conservé en localStorage, profil exposé sous forme de signal. */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  readonly user = signal<UserProfile | null>(this.restoreUser());
  readonly isLoggedIn = computed(() => this.user() !== null);
  readonly role = computed<Role | null>(() => this.user()?.role ?? null);
  readonly isStaff = computed(() => ['ADMIN', 'MANAGER', 'TECHNICIAN'].includes(this.role() ?? ''));
  readonly isManager = computed(() => ['ADMIN', 'MANAGER'].includes(this.role() ?? ''));
  readonly isAdmin = computed(() => this.role() === 'ADMIN');

  login(email: string, password: string) {
    return this.http.post<LoginResponse>(`${environment.apiUrl}/auth/login`, { email, password }).pipe(
      tap((res) => {
        localStorage.setItem(TOKEN_KEY, res.accessToken);
        localStorage.setItem(USER_KEY, JSON.stringify(res.user));
        this.user.set(res.user);
      }),
    );
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    this.user.set(null);
    this.router.navigateByUrl('/login');
  }

  token(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  hasAnyRole(roles: Role[]): boolean {
    const r = this.role();
    return r !== null && roles.includes(r);
  }

  private restoreUser(): UserProfile | null {
    try {
      const raw = localStorage.getItem(USER_KEY);
      const token = localStorage.getItem(TOKEN_KEY);
      if (!raw || !token) return null;
      // Expiration du jeton : on décode la charge utile (sans vérification de signature, faite côté serveur)
      const payload = JSON.parse(atob(token.split('.')[1]));
      if (payload.exp && payload.exp * 1000 < Date.now()) return null;
      return JSON.parse(raw) as UserProfile;
    } catch {
      return null;
    }
  }
}
