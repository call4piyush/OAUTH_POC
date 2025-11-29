import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from './auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule],
  template: `
    <main class="container">
      <header>
        <h1>OAuth2 BFF Demo</h1>
        <p>Angular talks only to the BFF, which handles tokens securely.</p>
      </header>

      <section class="card">
        <h2>Status</h2>
        <p *ngIf="auth.loading()">Loading profile...</p>
        <ng-container *ngIf="!auth.loading()">
          <p *ngIf="auth.profile(); else loggedOut">
            Logged in as <strong>{{ auth.profile()?.username }}</strong>
          </p>
          <ng-template #loggedOut>
            <p>You are not authenticated.</p>
          </ng-template>
        </ng-container>
        <p class="error" *ngIf="auth.error()">{{ auth.error() }}</p>
        <div class="actions">
          <button (click)="auth.login()">Login</button>
          <button (click)="auth.logout()" [disabled]="!auth.profile()">Logout</button>
          <button (click)="auth.hydrateProfile()">Refresh Profile</button>
        </div>
      </section>

      <section class="card" *ngIf="auth.profile() as profile">
        <h2>Profile</h2>
        <pre>{{ profile | json }}</pre>
      </section>
    </main>
  `,
  styles: [`
    :host {
      font-family: Inter, system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
      display: block;
      min-height: 100vh;
      background: #f5f6f8;
      color: #111;
    }
    .container {
      margin: 0 auto;
      max-width: 960px;
      padding: 2rem 1rem 4rem;
    }
    .card {
      background: white;
      border-radius: 16px;
      padding: 1.5rem;
      box-shadow: 0 30px 80px rgba(15, 23, 42, 0.08);
      margin-bottom: 1.5rem;
    }
    .actions {
      display: flex;
      gap: 1rem;
      flex-wrap: wrap;
    }
    button {
      border: none;
      border-radius: 999px;
      padding: 0.75rem 1.25rem;
      font-weight: 600;
      cursor: pointer;
      background: #2563eb;
      color: white;
    }
    button[disabled] {
      opacity: 0.5;
      cursor: not-allowed;
    }
    pre {
      background: #111827;
      color: #f8fafc;
      padding: 1rem;
      border-radius: 12px;
      overflow: auto;
    }
    .error {
      color: #dc2626;
    }
  `]
})
export class AppComponent implements OnInit {
  constructor(public auth: AuthService) {}

  ngOnInit(): void {
    this.auth.hydrateProfile();
  }
}


