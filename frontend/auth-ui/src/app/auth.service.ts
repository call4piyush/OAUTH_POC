import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../environments/environment';
import { tap } from 'rxjs/operators';

export interface UserProfile {
  id: number;
  subject: string;
  username: string;
  email: string;
  roles: string[];
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  readonly profile = signal<UserProfile | null>(null);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  constructor(private http: HttpClient) {}

  hydrateProfile() {
    this.loading.set(true);
    return this.http.get<UserProfile>(`${environment.bffBaseUrl}/session/me`)
      .pipe(
        tap({
          next: (profile) => {
            this.profile.set(profile);
            this.error.set(null);
            this.loading.set(false);
          },
          error: (err) => {
            if (err.status !== 401) {
              this.error.set('Failed to load profile');
            }
            this.profile.set(null);
            this.loading.set(false);
          }
        })
      ).subscribe();
  }

  login() {
    window.location.href = `${environment.bffBaseUrl}/auth/login`;
  }

  logout() {
    this.http.post(`${environment.bffBaseUrl}/auth/logout`, {})
      .subscribe({
        next: () => {
          this.profile.set(null);
        }
      });
  }
}


