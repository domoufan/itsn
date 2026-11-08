import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { NotifyService } from '../../core/notify.service';
import { Intervention, TicketSummary, UserAdmin } from '../../core/models';

/** Planning des interventions sur une semaine glissante, planification et compte rendu (exigences EF-08, EF-09). */
@Component({
  selector: 'app-planning',
  imports: [DatePipe, FormsModule, RouterLink, MatCardModule, MatButtonModule, MatIconModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatTableModule],
  template: `
    <div class="page">
      <div class="page-header"><h1>Planning des interventions</h1>
        <div class="toolbar-row">
          <button mat-stroked-button (click)="shift(-7)"><mat-icon>chevron_left</mat-icon></button>
          <strong>{{ from | date:'dd/MM' }} → {{ to | date:'dd/MM/yyyy' }}</strong>
          <button mat-stroked-button (click)="shift(7)"><mat-icon>chevron_right</mat-icon></button>
          <mat-form-field appearance="outline" subscriptSizing="dynamic"><mat-label>Technicien</mat-label>
            <mat-select [(ngModel)]="technicianId" (selectionChange)="load()"><mat-option [value]="null">Tous</mat-option>@for (u of technicians(); track u.id) { <mat-option [value]="u.id">{{ u.fullName }}</mat-option> }</mat-select></mat-form-field>
          <button mat-flat-button color="primary" (click)="showForm.set(!showForm())"><mat-icon>add</mat-icon> Planifier</button>
        </div>
      </div>

      @if (showForm()) {
        <mat-card style="margin-bottom:16px"><mat-card-content>
          <div class="form-grid">
            <mat-form-field appearance="outline"><mat-label>Ticket ouvert</mat-label>
              <mat-select [(ngModel)]="draft.ticketId">@for (t of openTickets(); track t.id) { <mat-option [value]="t.id">{{ t.reference }} – {{ t.title }}</mat-option> }</mat-select></mat-form-field>
            <mat-form-field appearance="outline"><mat-label>Technicien</mat-label>
              <mat-select [(ngModel)]="draft.technicianId">@for (u of technicians(); track u.id) { <mat-option [value]="u.id" [disabled]="auth.role() === 'TECHNICIAN' && u.id !== auth.user()?.id">{{ u.fullName }}</mat-option> }</mat-select></mat-form-field>
            <mat-form-field appearance="outline"><mat-label>Mode</mat-label>
              <mat-select [(ngModel)]="draft.mode"><mat-option value="ONSITE">Sur site</mat-option><mat-option value="REMOTE">À distance</mat-option></mat-select></mat-form-field>
            <mat-form-field appearance="outline"><mat-label>Début</mat-label><input matInput type="datetime-local" [(ngModel)]="draft.start" /></mat-form-field>
            <mat-form-field appearance="outline"><mat-label>Fin</mat-label><input matInput type="datetime-local" [(ngModel)]="draft.end" /></mat-form-field>
          </div>
          <button mat-flat-button color="primary" (click)="plan()">Enregistrer</button>
        </mat-card-content></mat-card>
      }

      <table mat-table [dataSource]="items()">
        <ng-container matColumnDef="when"><th mat-header-cell *matHeaderCellDef>Créneau</th><td mat-cell *matCellDef="let i">{{ i.scheduledStart | date:'EEE dd/MM HH:mm' }} → {{ i.scheduledEnd | date:'HH:mm' }}</td></ng-container>
        <ng-container matColumnDef="ticket"><th mat-header-cell *matHeaderCellDef>Ticket</th><td mat-cell *matCellDef="let i"><a [routerLink]="['/tickets', i.ticketId]" class="mono">{{ i.ticketReference }}</a> <span class="badge prio-{{ i.ticketPriority }}">{{ i.ticketPriority }}</span><div class="small muted">{{ i.ticketTitle }} · {{ i.siteName }}</div></td></ng-container>
        <ng-container matColumnDef="tech"><th mat-header-cell *matHeaderCellDef>Technicien</th><td mat-cell *matCellDef="let i">{{ i.technicianName }}</td></ng-container>
        <ng-container matColumnDef="mode"><th mat-header-cell *matHeaderCellDef>Mode</th><td mat-cell *matCellDef="let i">{{ i.mode === 'ONSITE' ? 'Sur site' : 'À distance' }}</td></ng-container>
        <ng-container matColumnDef="status"><th mat-header-cell *matHeaderCellDef>État</th><td mat-cell *matCellDef="let i"><span class="badge st-{{ i.status }}">{{ i.status }}</span></td></ng-container>
        <ng-container matColumnDef="actions"><th mat-header-cell *matHeaderCellDef></th><td mat-cell *matCellDef="let i">
          @if (i.status === 'PLANNED' || i.status === 'IN_PROGRESS') { <button mat-button (click)="reporting.set(i)">Compte rendu</button> }</td></ng-container>
        <tr mat-header-row *matHeaderRowDef="cols"></tr><tr mat-row *matRowDef="let r; columns: cols"></tr>
        <tr class="mat-row" *matNoDataRow><td class="mat-cell muted" [attr.colspan]="cols.length" style="padding:24px;text-align:center">Aucune intervention sur la période</td></tr>
      </table>

      @if (reporting(); as i) {
        <mat-card style="margin-top:16px"><mat-card-header><mat-card-title>Compte rendu — {{ i.ticketReference }}</mat-card-title></mat-card-header>
          <mat-card-content>
            <div class="form-grid">
              <mat-form-field appearance="outline"><mat-label>État</mat-label><mat-select [(ngModel)]="report.status"><mat-option value="IN_PROGRESS">En cours</mat-option><mat-option value="DONE">Terminée</mat-option><mat-option value="CANCELLED">Annulée</mat-option></mat-select></mat-form-field>
              <mat-form-field appearance="outline"><mat-label>Début réel</mat-label><input matInput type="datetime-local" [(ngModel)]="report.actualStart" /></mat-form-field>
              <mat-form-field appearance="outline"><mat-label>Fin réelle</mat-label><input matInput type="datetime-local" [(ngModel)]="report.actualEnd" /></mat-form-field>
              <mat-form-field appearance="outline"><mat-label>Pièces utilisées</mat-label><input matInput [(ngModel)]="report.partsUsed" /></mat-form-field>
            </div>
            <mat-form-field class="full" appearance="outline"><mat-label>Compte rendu (obligatoire pour terminer)</mat-label><textarea matInput rows="3" [(ngModel)]="report.report"></textarea></mat-form-field>
            <button mat-flat-button color="primary" (click)="sendReport(i)">Enregistrer</button> <button mat-button (click)="reporting.set(null)">Annuler</button>
          </mat-card-content></mat-card>
      }
    </div>
  `,
})
export class PlanningComponent {
  private readonly api = inject(ApiService);
  private readonly notify = inject(NotifyService);
  readonly auth = inject(AuthService);
  readonly cols = ['when', 'ticket', 'tech', 'mode', 'status', 'actions'];
  readonly items = signal<Intervention[]>([]);
  readonly technicians = signal<UserAdmin[]>([]);
  readonly openTickets = signal<TicketSummary[]>([]);
  readonly showForm = signal(false);
  readonly reporting = signal<Intervention | null>(null);
  technicianId: number | null = null;
  from = startOfWeek(new Date()); to = addDays(this.from, 7);
  draft = { ticketId: null as number | null, technicianId: this.auth.user()?.id ?? null, mode: 'ONSITE', start: '', end: '' };
  report = { status: 'DONE', actualStart: '', actualEnd: '', report: '', partsUsed: '' };

  constructor() {
    this.api.technicians().subscribe((t) => this.technicians.set(t));
    this.api.tickets({ openOnly: true, size: 100 }).subscribe((p) => this.openTickets.set(p.content));
    this.load();
  }
  load() { this.api.interventions(this.from.toISOString(), this.to.toISOString(), this.technicianId ?? undefined).subscribe((i) => this.items.set(i)); }
  shift(days: number) { this.from = addDays(this.from, days); this.to = addDays(this.to, days); this.load(); }
  plan() {
    const d = this.draft;
    if (!d.ticketId || !d.technicianId || !d.start || !d.end) { this.notify.error(null, 'Tous les champs sont requis'); return; }
    this.api.planIntervention({ ticketId: d.ticketId, technicianId: d.technicianId, mode: d.mode, scheduledStart: new Date(d.start).toISOString(), scheduledEnd: new Date(d.end).toISOString() })
      .subscribe({ next: () => { this.notify.success('Intervention planifiée'); this.showForm.set(false); this.load(); }, error: (e) => this.notify.error(e) });
  }
  sendReport(i: Intervention) {
    const r = this.report;
    this.api.reportIntervention(i.id, { status: r.status, actualStart: r.actualStart ? new Date(r.actualStart).toISOString() : null, actualEnd: r.actualEnd ? new Date(r.actualEnd).toISOString() : null, report: r.report, partsUsed: r.partsUsed })
      .subscribe({ next: () => { this.notify.success('Compte rendu enregistré'); this.reporting.set(null); this.load(); }, error: (e) => this.notify.error(e) });
  }
}
function startOfWeek(d: Date): Date { const x = new Date(d); x.setHours(0, 0, 0, 0); x.setDate(x.getDate() - ((x.getDay() + 6) % 7)); return x; }
function addDays(d: Date, n: number): Date { const x = new Date(d); x.setDate(x.getDate() + n); return x; }
