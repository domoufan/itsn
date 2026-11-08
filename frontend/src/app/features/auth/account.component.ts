import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { NotifyService } from '../../core/notify.service';
import { RoleLabelPipe } from '../../core/pipes';

@Component({
  selector: 'app-account',
  imports: [ReactiveFormsModule, MatCardModule, MatFormFieldModule, MatInputModule, MatButtonModule, RoleLabelPipe],
  template: `
    <div class="page">
      <div class="page-header"><h1>Mon compte</h1></div>
      <div class="grid grid-2">
        <mat-card>
          <mat-card-header><mat-card-title>Profil</mat-card-title></mat-card-header>
          <mat-card-content>
            <p><strong>{{ auth.user()?.fullName }}</strong></p>
            <p>{{ auth.user()?.email }}</p>
            <p>Rôle : {{ auth.role() | roleLabel }}</p>
            @if (auth.user()?.siteName) { <p>Site : {{ auth.user()?.siteName }}</p> }
          </mat-card-content>
        </mat-card>
        <mat-card>
          <mat-card-header><mat-card-title>Changer le mot de passe</mat-card-title></mat-card-header>
          <mat-card-content>
            <form [formGroup]="form" (ngSubmit)="submit()">
              <mat-form-field class="full" appearance="outline"><mat-label>Mot de passe actuel</mat-label><input matInput type="password" formControlName="currentPassword" autocomplete="current-password" /></mat-form-field>
              <mat-form-field class="full" appearance="outline">
                <mat-label>Nouveau mot de passe</mat-label><input matInput type="password" formControlName="newPassword" autocomplete="new-password" />
                <mat-hint>10 caractères minimum, avec majuscule, minuscule et chiffre</mat-hint>
              </mat-form-field>
              <button mat-flat-button color="primary" type="submit" [disabled]="form.invalid">Enregistrer</button>
            </form>
          </mat-card-content>
        </mat-card>
      </div>
    </div>
  `,
})
export class AccountComponent {
  readonly auth = inject(AuthService);
  private readonly api = inject(ApiService);
  private readonly notify = inject(NotifyService);
  private readonly fb = inject(FormBuilder);
  readonly form = this.fb.nonNullable.group({
    currentPassword: ['', Validators.required],
    newPassword: ['', [Validators.required, Validators.minLength(10), Validators.pattern(/^(?=.*[A-Z])(?=.*[a-z])(?=.*\d).+$/)]],
  });

  submit() {
    const v = this.form.getRawValue();
    this.api.changePassword(v.currentPassword, v.newPassword).subscribe({
      next: () => { this.notify.success('Mot de passe modifié'); this.form.reset(); },
      error: (e) => this.notify.error(e),
    });
  }
}
