import { Component, inject, signal } from '@angular/core';
import { DatePipe, KeyValuePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatIconModule } from '@angular/material/icon';
import { ApiService } from '../../core/api.service';
import { Kpis } from '../../core/models';
import { PriorityLabelPipe } from '../../core/pipes';

/** Indicateurs de pilotage (exigence EF-10) : volumes, SLA, MTTR, charge, répartition. */
@Component({
  selector: 'app-dashboard',
  imports: [DatePipe, KeyValuePipe, FormsModule, RouterLink, MatCardModule, MatSelectModule, MatFormFieldModule, MatProgressBarModule, MatIconModule, PriorityLabelPipe],
  template: `
    <div class="page">
      <div class="page-header">
        <h1>Tableau de bord</h1>
        <mat-form-field appearance="outline" subscriptSizing="dynamic"><mat-label>Période</mat-label>
          <mat-select [(ngModel)]="days" (selectionChange)="load()"><mat-option [value]="7">7 jours</mat-option><mat-option [value]="30">30 jours</mat-option><mat-option [value]="90">90 jours</mat-option></mat-select></mat-form-field>
      </div>
      @if (kpis(); as k) {
        <div class="grid grid-4">
          <mat-card class="kpi-card"><div class="kpi-value">{{ k.openTickets }}</div><div class="kpi-label">Tickets ouverts</div></mat-card>
          <mat-card class="kpi-card"><div class="kpi-value" [class.sla-breach]="k.overdueOpen > 0">{{ k.overdueOpen }}</div><div class="kpi-label">Ouverts hors délai</div></mat-card>
          <mat-card class="kpi-card"><div class="kpi-value">{{ k.createdLast30Days }} / {{ k.resolvedLast30Days }}</div><div class="kpi-label">Créés / résolus sur {{ days }} j</div></mat-card>
          <mat-card class="kpi-card"><div class="kpi-value">{{ k.meanTimeToResolveHours ?? '—' }} h</div><div class="kpi-label">Délai moyen de résolution (MTTR)</div></mat-card>
          <mat-card class="kpi-card"><div class="kpi-value">{{ k.slaResolutionComplianceRate }} %</div><div class="kpi-label">Respect SLA résolution</div><mat-progress-bar mode="determinate" [value]="k.slaResolutionComplianceRate" /></mat-card>
          <mat-card class="kpi-card"><div class="kpi-value">{{ k.slaResponseComplianceRate }} %</div><div class="kpi-label">Respect SLA prise en compte</div><mat-progress-bar mode="determinate" [value]="k.slaResponseComplianceRate" /></mat-card>
          <mat-card class="kpi-card"><div class="kpi-value">{{ k.averageSatisfaction ?? '—' }} / 5</div><div class="kpi-label">Satisfaction moyenne</div></mat-card>
          <mat-card class="kpi-card clickable" routerLink="/ai"><div class="kpi-value">{{ k.pendingAiSuggestions }}</div><div class="kpi-label"><mat-icon inline>smart_toy</mat-icon> Propositions IA à valider</div></mat-card>
        </div>
        <div class="grid grid-2" style="margin-top:16px">
          <mat-card><mat-card-header><mat-card-title>Tickets ouverts par priorité</mat-card-title></mat-card-header>
            <mat-card-content>
              @for (p of ['P1','P2','P3','P4']; track p) {
                <div class="bar-row"><span class="badge prio-{{ p }}" style="width:110px">{{ p | priorityLabel }}</span>
                  <div class="bar"><div class="bar-fill prio-{{ p }}" [style.width.%]="pct(k.openByPriority[p] || 0, k.openTickets)"></div></div><strong>{{ k.openByPriority[p] || 0 }}</strong></div>
              }
            </mat-card-content></mat-card>
          <mat-card><mat-card-header><mat-card-title>Charge des techniciens (tickets actifs)</mat-card-title></mat-card-header>
            <mat-card-content>
              @for (w of k.workload; track w.technicianId) {
                <div class="bar-row"><span style="width:160px">{{ w.technicianName }}</span><div class="bar"><div class="bar-fill" style="background:#1d4ed8" [style.width.%]="pct(w.activeTickets, maxLoad(k))"></div></div><strong>{{ w.activeTickets }}</strong></div>
              } @empty { <p class="muted">Aucun ticket actif affecté.</p> }
            </mat-card-content></mat-card>
          <mat-card><mat-card-header><mat-card-title>Répartition par catégorie ({{ days }} j)</mat-card-title></mat-card-header>
            <mat-card-content>
              @for (c of k.byCategory | keyvalue: byValueDesc; track c.key) {
                <div class="bar-row"><span style="width:160px">{{ c.key }}</span><div class="bar"><div class="bar-fill" style="background:#0f766e" [style.width.%]="pct(c.value, k.createdLast30Days)"></div></div><strong>{{ c.value }}</strong></div>
              }
            </mat-card-content></mat-card>
        </div>
        <p class="small muted">Calculé le {{ k.computedAt | date:'dd/MM/yyyy HH:mm:ss' }}</p>
      }
    </div>
  `,
  styles: [`
    .bar-row { display: flex; align-items: center; gap: 12px; margin: 8px 0; }
    .bar { flex: 1; height: 12px; background: #eef2f7; border-radius: 6px; overflow: hidden; }
    .bar-fill { height: 100%; border-radius: 6px; transition: width .3s; }
    .bar-fill.prio-P1 { background: #dc2626; } .bar-fill.prio-P2 { background: #f59e0b; } .bar-fill.prio-P3 { background: #3b82f6; } .bar-fill.prio-P4 { background: #16a34a; }
  `],
})
export class DashboardComponent {
  private readonly api = inject(ApiService);
  readonly kpis = signal<Kpis | null>(null);
  days = 30;
  constructor() { this.load(); }
  load() { this.api.kpis(this.days).subscribe((k) => this.kpis.set(k)); }
  pct(v: number, total: number) { return total > 0 ? Math.round((100 * v) / total) : 0; }
  maxLoad(k: Kpis) { return Math.max(1, ...k.workload.map((w) => w.activeTickets)); }
  byValueDesc = (a: { value: number }, b: { value: number }) => b.value - a.value;
}
