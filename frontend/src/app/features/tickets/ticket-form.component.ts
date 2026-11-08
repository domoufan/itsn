import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatRadioModule } from '@angular/material/radio';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { NotifyService } from '../../core/notify.service';
import { Category, Equipment, SCALE_LABELS, Site, UserAdmin } from '../../core/models';

/** Création d'un ticket ; la priorité est affichée en direct à partir de la matrice impact x urgence. */
@Component({
  selector: 'app-ticket-form',
  imports: [ReactiveFormsModule, MatCardModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatButtonModule, MatRadioModule],
  template: `
    <div class="page" style="max-width:900px">
      <div class="page-header"><h1>Nouveau ticket</h1></div>
      <mat-card>
        <mat-card-content>
          <form [formGroup]="form" (ngSubmit)="submit()">
            <mat-radio-group formControlName="type" style="display:flex;gap:24px;margin-bottom:12px">
              <mat-radio-button value="INCIDENT">Incident (quelque chose ne fonctionne plus)</mat-radio-button>
              <mat-radio-button value="REQUEST">Demande (installation, accès, matériel…)</mat-radio-button>
            </mat-radio-group>
            <mat-form-field class="full" appearance="outline"><mat-label>Titre</mat-label><input matInput formControlName="title" maxlength="160" /><mat-hint align="end">{{ form.controls.title.value.length }}/160</mat-hint></mat-form-field>
            <mat-form-field class="full" appearance="outline"><mat-label>Description détaillée</mat-label><textarea matInput rows="6" formControlName="description" placeholder="Symptômes, depuis quand, nombre de personnes concernées, actions déjà tentées…"></textarea></mat-form-field>
            <div class="form-grid">
              <mat-form-field appearance="outline"><mat-label>Catégorie</mat-label>
                <mat-select formControlName="categoryId"><mat-option [value]="null">Je ne sais pas</mat-option>@for (c of categories(); track c.id) { <mat-option [value]="c.id">{{ c.name }}</mat-option> }</mat-select></mat-form-field>
              <mat-form-field appearance="outline"><mat-label>Impact (qui est touché ?)</mat-label>
                <mat-select formControlName="impact">@for (k of [1,2,3]; track k) { <mat-option [value]="k">{{ scale[k] }}</mat-option> }</mat-select></mat-form-field>
              <mat-form-field appearance="outline"><mat-label>Urgence (quand ?)</mat-label>
                <mat-select formControlName="urgency">@for (k of [1,2,3]; track k) { <mat-option [value]="k">{{ scale[k] }}</mat-option> }</mat-select></mat-form-field>
              @if (auth.isStaff()) {
                <mat-form-field appearance="outline"><mat-label>Site</mat-label>
                  <mat-select formControlName="siteId" (selectionChange)="loadEquipment()"><mat-option [value]="null">—</mat-option>@for (s of sites(); track s.id) { <mat-option [value]="s.id">{{ s.name }} – {{ s.clientName }}</mat-option> }</mat-select></mat-form-field>
                <mat-form-field appearance="outline"><mat-label>Pour le compte de</mat-label>
                  <mat-select formControlName="requesterId"><mat-option [value]="null">Moi-même</mat-option>@for (u of requesters(); track u.id) { <mat-option [value]="u.id">{{ u.fullName }} ({{ u.siteName }})</mat-option> }</mat-select></mat-form-field>
              }
              <mat-form-field appearance="outline"><mat-label>Équipement concerné</mat-label>
                <mat-select formControlName="equipmentId"><mat-option [value]="null">Aucun / inconnu</mat-option>@for (e of equipment(); track e.id) { <mat-option [value]="e.id">{{ e.assetTag }} – {{ e.brand }} {{ e.model }}</mat-option> }</mat-select></mat-form-field>
            </div>
            <p>Priorité calculée : <span class="badge prio-{{ priority() }}">{{ priority() }}</span> <span class="small muted">(matrice impact × urgence, ajustable par le support)</span></p>
            <div style="display:flex;gap:12px;justify-content:flex-end">
              <button mat-button type="button" (click)="router.navigateByUrl('/tickets')">Annuler</button>
              <button mat-flat-button color="primary" type="submit" [disabled]="form.invalid || saving()">Créer le ticket</button>
            </div>
          </form>
        </mat-card-content>
      </mat-card>
    </div>
  `,
})
export class TicketFormComponent {
  private readonly api = inject(ApiService);
  private readonly notify = inject(NotifyService);
  private readonly fb = inject(FormBuilder);
  readonly auth = inject(AuthService);
  readonly router = inject(Router);

  readonly scale = SCALE_LABELS;
  readonly categories = signal<Category[]>([]);
  readonly sites = signal<Site[]>([]);
  readonly equipment = signal<Equipment[]>([]);
  readonly requesters = signal<UserAdmin[]>([]);
  readonly saving = signal(false);
  readonly priority = signal('P3');

  readonly form = this.fb.nonNullable.group({
    type: ['INCIDENT', Validators.required],
    title: ['', [Validators.required, Validators.minLength(5), Validators.maxLength(160)]],
    description: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(5000)]],
    categoryId: [null as number | null],
    impact: [2, Validators.required],
    urgency: [2, Validators.required],
    siteId: [null as number | null],
    requesterId: [null as number | null],
    equipmentId: [null as number | null],
  });

  constructor() {
    this.api.categories().subscribe((c) => this.categories.set(c));
    const siteId = this.auth.user()?.siteId;
    if (this.auth.isStaff()) {
      this.api.sites().subscribe((s) => this.sites.set(s));
      this.api.users('REQUESTER').subscribe((u) => this.requesters.set(u));
      this.api.equipment().subscribe((e) => this.equipment.set(e));
    } else if (siteId) {
      this.api.equipment(siteId).subscribe((e) => this.equipment.set(e));
    }
    this.form.valueChanges.subscribe((v) => this.priority.set(this.compute(v.impact ?? 2, v.urgency ?? 2)));
  }

  loadEquipment() {
    const siteId = this.form.controls.siteId.value ?? undefined;
    this.api.equipment(siteId).subscribe((e) => this.equipment.set(e));
  }

  /** Même matrice que le serveur (PriorityMatrix.java) : affichage indicatif, la valeur de référence est calculée côté API. */
  compute(impact: number, urgency: number): string {
    const s = impact + urgency;
    return s <= 2 ? 'P1' : s === 3 ? 'P2' : s === 4 ? 'P3' : 'P4';
  }

  submit() {
    if (this.form.invalid) return;
    this.saving.set(true);
    this.api.createTicket(this.form.getRawValue()).subscribe({
      next: (t) => { this.notify.success(`Ticket ${t.reference} créé`); this.router.navigate(['/tickets', t.id]); },
      error: (e) => { this.saving.set(false); this.notify.error(e); },
    });
  }
}
