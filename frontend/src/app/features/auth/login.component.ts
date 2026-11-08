import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { AuthService } from '../../core/auth.service';
import { NotifyService } from '../../core/notify.service';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, MatCardModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatProgressBarModule],
  template: `
    <div class="login-wrap">
      <mat-card class="login-card">
        <mat-card-header>
          <mat-card-title>IntervIT</mat-card-title>
          <mat-card-subtitle>Connexion à la plateforme de gestion des interventions</mat-card-subtitle>
        </mat-card-header>
        @if (loading()) { <mat-progress-bar mode="indeterminate" /> }
        <mat-card-content>
          <form [formGroup]="form" (ngSubmit)="submit()">
            <mat-form-field class="full" appearance="outline">
              <mat-label>Adresse e-mail</mat-label>
              <input matInput type="email" formControlName="email" autocomplete="username" />
            </mat-form-field>
            <mat-form-field class="full" appearance="outline">
              <mat-label>Mot de passe</mat-label>
              <input matInput type="password" formControlName="password" autocomplete="current-password" />
            </mat-form-field>
            <button mat-flat-button color="primary" class="full" type="submit" [disabled]="form.invalid || loading()">Se connecter</button>
          </form>
          <p class="small muted" style="margin-top:16px">
            Comptes de démonstration (mot de passe commun : <span class="mono">Demo&#64;2026!</span>) :
            admin&#64;senetech.example · manager&#64;senetech.example · tech.reseau&#64;senetech.example · user&#64;client.example
          </p>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .login-wrap { min-height: 100vh; display: grid; place-items: center; background: linear-gradient(135deg, #0f2140, #1d4ed8); }
    .login-card { width: min(440px, 92vw); padding: 8px; }
  `],
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly notify = inject(NotifyService);

  readonly loading = signal(false);
  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
  });

  submit() {
    if (this.form.invalid) return;
    this.loading.set(true);
    const { email, password } = this.form.getRawValue();
    this.auth.login(email, password).subscribe({
      next: () => {
        const redirect = this.route.snapshot.queryParamMap.get('redirect');
        this.router.navigateByUrl(redirect && redirect !== '/login' ? redirect : (this.auth.isStaff() ? '/dashboard' : '/tickets'));
      },
      error: (e) => { this.loading.set(false); this.notify.error(e, 'Connexion refusée'); },
    });
  }
}
