import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { AuthService } from '../../core/auth.service';
import { NotifyService } from '../../core/notify.service';
import { LogoComponent } from '../../shared/ui';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatIconModule, MatProgressBarModule, LogoComponent],
  template: `
    <div class="login">
      <div class="blob b1"></div><div class="blob b2"></div><div class="blob b3"></div>
      <section class="hero">
        <itsn-logo [size]="64" />
        <h1>ITSN</h1>
        <p class="tag">Interventions Techniques &amp; Services Numériques</p>
        <ul class="feats">
          <li><mat-icon>bolt</mat-icon> Tickets priorisés et engagements de service suivis</li>
          <li><mat-icon>event_available</mat-icon> Planning des interventions sans conflit</li>
          <li><mat-icon>smart_toy</mat-icon> Triage assisté par IA, décision humaine</li>
          <li><mat-icon>insights</mat-icon> Indicateurs de pilotage en temps réel</li>
        </ul>
      </section>
      <section class="card">
        @if (loading()) { <mat-progress-bar mode="indeterminate" /> }
        <h2>Bon retour 👋</h2>
        <p class="muted">Connectez-vous pour accéder à votre espace.</p>
        <form [formGroup]="form" (ngSubmit)="submit()">
          <mat-form-field class="full" appearance="outline">
            <mat-label>Adresse e-mail</mat-label>
            <mat-icon matPrefix>mail</mat-icon>
            <input matInput type="email" formControlName="email" autocomplete="username" />
          </mat-form-field>
          <mat-form-field class="full" appearance="outline">
            <mat-label>Mot de passe</mat-label>
            <mat-icon matPrefix>lock</mat-icon>
            <input matInput [type]="show() ? 'text' : 'password'" formControlName="password" autocomplete="current-password" />
            <button mat-icon-button matSuffix type="button" (click)="show.set(!show())"><mat-icon>{{ show() ? 'visibility_off' : 'visibility' }}</mat-icon></button>
          </mat-form-field>
          <button mat-flat-button color="primary" class="full big" type="submit" [disabled]="form.invalid || loading()">Se connecter <mat-icon>arrow_forward</mat-icon></button>
        </form>
        <div class="demo">
          <div class="small muted">Comptes de démonstration · mot de passe <span class="mono">Demo&#64;2026!</span></div>
          <div class="demo-chips">
            @for (d of demos; track d.email) { <button type="button" class="chip" (click)="fill(d.email)">{{ d.label }}</button> }
          </div>
        </div>
      </section>
    </div>
  `,
  styles: [`
    .login { min-height: 100vh; display: grid; grid-template-columns: 1.1fr 1fr; background: radial-gradient(1200px 600px at 10% 10%, #0B4A3E 0%, var(--itsn-navy-800) 45%, var(--itsn-navy-900) 100%); position: relative; overflow: hidden; }
    .blob { position: absolute; border-radius: 50%; filter: blur(60px); opacity: .45; animation: floatY 9s ease-in-out infinite; }
    .b1 { width: 420px; height: 420px; background: var(--itsn-green-300); left: -120px; bottom: -140px; }
    .b2 { width: 300px; height: 300px; background: var(--itsn-green-500); left: 40%; top: -120px; animation-delay: -3s; }
    .b3 { width: 260px; height: 260px; background: #2F80ED; right: 10%; bottom: 10%; animation-delay: -6s; opacity: .25; }
    .hero { position: relative; color: #fff; padding: 9vh 8vw; display: flex; flex-direction: column; justify-content: center; animation: pageIn .8s var(--itsn-ease) both; }
    .hero h1 { font-size: 3.4rem; letter-spacing: .12em; margin: 18px 0 4px; font-weight: 800; }
    .tag { color: var(--itsn-green-300); font-weight: 600; margin: 0 0 32px; font-size: 1.05rem; }
    .feats { list-style: none; padding: 0; margin: 0; display: grid; gap: 14px; }
    .feats li { display: flex; align-items: center; gap: 12px; color: #D6E8DF; font-size: 1rem; animation: rowIn .6s var(--itsn-ease) both; }
    .feats li:nth-child(2) { animation-delay: .1s } .feats li:nth-child(3) { animation-delay: .2s } .feats li:nth-child(4) { animation-delay: .3s }
    .feats mat-icon { background: rgba(98,216,78,.18); color: var(--itsn-green-300); border-radius: 10px; padding: 6px; width: 36px; height: 36px; box-sizing: border-box; }
    .card { position: relative; margin: auto; width: min(440px, 90vw); background: #fff; border-radius: 22px; padding: 36px 34px 28px; box-shadow: 0 30px 80px rgba(0,0,0,.35); animation: cardIn .7s .15s var(--itsn-ease) both; }
    .card h2 { margin: 4px 0 2px; color: var(--itsn-navy-800); }
    .card p { margin: 0 0 20px; }
    .big { height: 48px !important; font-size: 1rem !important; }
    .demo { margin-top: 22px; border-top: 1px dashed var(--itsn-line); padding-top: 16px; }
    .demo-chips { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 10px; }
    .chip { border: 1px solid var(--itsn-green-100); background: var(--itsn-green-50); color: var(--itsn-green-700); border-radius: 999px; padding: 5px 12px; font-size: .8rem; font-weight: 600; cursor: pointer; transition: all var(--itsn-fast); }
    .chip:hover { background: var(--itsn-green-100); transform: translateY(-1px); }
    @media (max-width: 900px) { .login { grid-template-columns: 1fr; } .hero { padding: 6vh 8vw 2vh; } .card { margin: 16px auto 40px; } }
  `],
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly notify = inject(NotifyService);
  readonly loading = signal(false);
  readonly show = signal(false);
  readonly demos = [
    { label: 'Administrateur', email: 'admin@senetech.example' }, { label: 'Responsable', email: 'manager@senetech.example' },
    { label: 'Technicien', email: 'tech.reseau@senetech.example' }, { label: 'Demandeur', email: 'user@client.example' },
  ];
  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
  });
  fill(email: string) { this.form.patchValue({ email, password: 'Demo@2026!' }); }
  submit() {
    if (this.form.invalid) return;
    this.loading.set(true);
    const { email, password } = this.form.getRawValue();
    this.auth.login(email, password).subscribe({
      next: () => { const r = this.route.snapshot.queryParamMap.get('redirect'); this.router.navigateByUrl(r && r !== '/login' ? r : (this.auth.isStaff() ? '/dashboard' : '/tickets')); },
      error: (e) => { this.loading.set(false); this.notify.error(e, 'Connexion refusée'); },
    });
  }
}
