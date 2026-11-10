import { Component, computed, inject, signal } from '@angular/core';
import { DatePipe, KeyValuePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { Kpis, Page, TicketSummary } from '../../core/models';
import { PriorityLabelPipe, StatusLabelPipe, DueInPipe } from '../../core/pipes';
import { CountUpDirective, DonutComponent, DonutSlice, RingComponent, EmptyComponent } from '../../shared/ui';

/** Tableau de bord (EF-10) : indicateurs animés, anneaux SLA, répartition en donut, charge, tickets urgents. */
@Component({
  selector: 'app-dashboard',
  imports: [DatePipe, KeyValuePipe, FormsModule, RouterLink, MatCardModule, MatSelectModule, MatFormFieldModule, MatIconModule, MatButtonModule, PriorityLabelPipe, StatusLabelPipe, DueInPipe, CountUpDirective, DonutComponent, RingComponent, EmptyComponent],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>{{ greeting() }}, {{ firstName() }} 👋</h1>
          <p class="subtitle">Voici l'état du support {{ periodLabel() }}.</p>
        </div>
        <div class="toolbar-row" style="margin:0">
          <mat-form-field appearance="outline" subscriptSizing="dynamic"><mat-label>Période</mat-label>
            <mat-select [(ngModel)]="days" (selectionChange)="load()"><mat-option [value]="7">7 jours</mat-option><mat-option [value]="30">30 jours</mat-option><mat-option [value]="90">90 jours</mat-option></mat-select></mat-form-field>
          <button mat-stroked-button (click)="load()"><mat-icon>refresh</mat-icon> Actualiser</button>
        </div>
      </div>

      @if (kpis(); as k) {
        <div class="grid grid-4 stagger">
          <mat-card class="kpi-card lift"><span class="kpi-icon info"><mat-icon>confirmation_number</mat-icon></span><div class="kpi-value" [countUp]="k.openTickets"></div><div class="kpi-label">Tickets ouverts</div></mat-card>
          <mat-card class="kpi-card lift"><span class="kpi-icon" [class.danger]="k.overdueOpen > 0"><mat-icon>alarm</mat-icon></span><div class="kpi-value" [class.sla-breach]="k.overdueOpen > 0" [countUp]="k.overdueOpen"></div><div class="kpi-label">Ouverts hors délai</div></mat-card>
          <mat-card class="kpi-card lift"><span class="kpi-icon"><mat-icon>swap_vert</mat-icon></span><div class="kpi-value"><span [countUp]="k.createdLast30Days"></span> <span class="muted" style="font-size:1.1rem">/</span> <span [countUp]="k.resolvedLast30Days"></span></div><div class="kpi-label">Créés / résolus sur {{ days }} j</div></mat-card>
          <mat-card class="kpi-card lift"><span class="kpi-icon warn"><mat-icon>timer</mat-icon></span><div class="kpi-value" [countUp]="k.meanTimeToResolveHours" [decimals]="1" suffix=" h"></div><div class="kpi-label">Délai moyen de résolution</div></mat-card>
        </div>

        <div class="grid grid-3 stagger" style="margin-top:18px">
          <mat-card class="lift ring-card">
            <itsn-ring [value]="k.slaResolutionComplianceRate" [label]="k.slaResolutionComplianceRate + ' %'" sub="RÉSOLUTION" />
            <div><h3>Respect du SLA de résolution</h3><p class="muted small">Tickets résolus dans le délai de leur priorité, sur la période.</p></div>
          </mat-card>
          <mat-card class="lift ring-card">
            <itsn-ring [value]="k.slaResponseComplianceRate" [label]="k.slaResponseComplianceRate + ' %'" sub="PRISE EN COMPTE" />
            <div><h3>Respect du SLA de prise en compte</h3><p class="muted small">Première réponse du support dans le délai attendu.</p></div>
          </mat-card>
          <mat-card class="lift ring-card">
            <itsn-ring [value]="k.averageSatisfaction ? k.averageSatisfaction * 20 : 0" [label]="k.averageSatisfaction ? k.averageSatisfaction + '/5' : '—'" sub="SATISFACTION" />
            <div><h3>Satisfaction des demandeurs</h3><p class="muted small">Note moyenne attribuée après résolution.</p></div>
          </mat-card>
        </div>

        <div class="grid grid-2 stagger" style="margin-top:18px">
          <mat-card class="lift"><mat-card-header><mat-card-title>Tickets ouverts par priorité</mat-card-title></mat-card-header>
            <mat-card-content><itsn-donut [slices]="prioritySlices()" centerLabel="ouverts" /></mat-card-content></mat-card>
          <mat-card class="lift"><mat-card-header><mat-card-title>Charge des techniciens</mat-card-title><mat-card-subtitle>Tickets affectés, en cours ou en attente</mat-card-subtitle></mat-card-header>
            <mat-card-content>
              @for (w of k.workload; track w.technicianId) {
                <div class="bar-row"><span class="who"><span class="av">{{ w.technicianName[0] }}</span>{{ w.technicianName }}</span>
                  <div class="bar"><div class="bar-fill" [style.width.%]="pct(w.activeTickets, maxLoad(k))" [style.background]="loadColor(w.activeTickets)"></div></div><strong>{{ w.activeTickets }}</strong></div>
              } @empty { <itsn-empty title="Aucune charge affectée" text="Les tickets ouverts attendent une affectation." /> }
            </mat-card-content></mat-card>
          <mat-card class="lift"><mat-card-header><mat-card-title>Répartition par catégorie</mat-card-title><mat-card-subtitle>Tickets créés sur {{ days }} jours</mat-card-subtitle></mat-card-header>
            <mat-card-content>
              @for (c of k.byCategory | keyvalue: byValueDesc; track c.key) {
                <div class="bar-row"><span class="who">{{ c.key }}</span><div class="bar"><div class="bar-fill cat" [style.width.%]="pct(c.value, k.createdLast30Days)"></div></div><strong>{{ c.value }}</strong></div>
              }
            </mat-card-content></mat-card>
          <mat-card class="lift">
            <mat-card-header><mat-card-title>À traiter en priorité</mat-card-title><mat-card-subtitle>Tickets ouverts les plus critiques</mat-card-subtitle></mat-card-header>
            <mat-card-content>
              @for (t of urgent(); track t.id) {
                <a class="urgent-row" [routerLink]="['/tickets', t.id]">
                  <span class="badge prio-{{ t.priority }}">{{ t.priority }}</span>
                  <span class="ut"><strong>{{ t.title }}</strong><span class="small muted">{{ t.reference }} · {{ t.site?.name }} · {{ t.status | statusLabel }}</span></span>
                  <span class="small" [class.sla-breach]="t.slaResolutionBreached">{{ t.resolutionDueAt | dueIn }}</span>
                </a>
              } @empty { <itsn-empty title="Tout est sous contrôle" text="Aucun ticket ouvert en ce moment." /> }
              @if (k.pendingAiSuggestions > 0 && auth.isManager()) {
                <a mat-stroked-button routerLink="/ai" class="full" style="margin-top:10px"><mat-icon>smart_toy</mat-icon> {{ k.pendingAiSuggestions }} proposition(s) IA à valider</a>
              }
            </mat-card-content>
          </mat-card>
        </div>
        <p class="small muted" style="margin-top:14px">Calculé le {{ k.computedAt | date:'dd/MM/yyyy à HH:mm:ss' }}</p>
      } @else {
        <div class="grid grid-4">@for (i of [1,2,3,4]; track i) { <mat-card class="kpi-card"><div class="skeleton" style="width:40%;height:34px"></div><div class="skeleton" style="width:70%;margin-top:12px"></div></mat-card> }</div>
      }
    </div>
  `,
  styles: [`
    .ring-card { display: flex; align-items: center; gap: 16px; padding: 16px 18px; }
    .ring-card h3 { margin: 0 0 4px; font-size: 1rem; color: var(--itsn-navy-800); }
    .ring-card p { margin: 0; }
    .bar-row { display: flex; align-items: center; gap: 12px; margin: 10px 0; }
    .who { width: 170px; display: flex; align-items: center; gap: 8px; font-size: .92rem; }
    .av { width: 26px; height: 26px; border-radius: 50%; background: var(--itsn-navy-800); color: #fff; display: grid; place-items: center; font-size: .75rem; font-weight: 700; }
    .bar { flex: 1; height: 12px; background: #EEF3F0; border-radius: 6px; overflow: hidden; }
    .bar-fill { height: 100%; border-radius: 6px; animation: growBar 1s var(--itsn-ease) both; background: var(--itsn-green-500); }
    .bar-fill.cat { background: linear-gradient(90deg, var(--itsn-navy-700), var(--itsn-green-500)); }
    .urgent-row { display: grid; grid-template-columns: auto 1fr auto; gap: 12px; align-items: center; padding: 10px 12px; border-radius: 12px; text-decoration: none; color: inherit; transition: background var(--itsn-fast), transform var(--itsn-fast); }
    .urgent-row:hover { background: var(--itsn-green-50); transform: translateX(3px); text-decoration: none; }
    .ut { display: flex; flex-direction: column; min-width: 0; }
    .ut strong { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
  `],
})
export class DashboardComponent {
  private readonly api = inject(ApiService);
  readonly auth = inject(AuthService);
  readonly kpis = signal<Kpis | null>(null);
  readonly urgent = signal<TicketSummary[]>([]);
  days = 30;
  constructor() { this.load(); }
  load() {
    this.kpis.set(null);
    this.api.kpis(this.days).subscribe((k) => this.kpis.set(k));
    this.api.tickets({ openOnly: true, size: 5, sort: 'priority,asc' }).subscribe((p: Page<TicketSummary>) => this.urgent.set(p.content));
  }
  readonly prioritySlices = computed<DonutSlice[]>(() => {
    const k = this.kpis(); if (!k) return [];
    const colors: Record<string, string> = { P1: '#D64545', P2: '#E8912D', P3: '#2F80ED', P4: '#27AE60' };
    return ['P1', 'P2', 'P3', 'P4'].map((p) => ({ label: p === 'P1' ? 'P1 Critique' : p === 'P2' ? 'P2 Élevée' : p === 'P3' ? 'P3 Moyenne' : 'P4 Faible', value: k.openByPriority[p] || 0, color: colors[p] }));
  });
  pct(v: number, total: number) { return total > 0 ? Math.round((100 * v) / total) : 0; }
  maxLoad(k: Kpis) { return Math.max(1, ...k.workload.map((w) => w.activeTickets)); }
  loadColor(n: number) { return n >= 5 ? 'var(--itsn-p1)' : n >= 3 ? 'var(--itsn-p2)' : 'var(--itsn-green-500)'; }
  byValueDesc = (a: { value: number }, b: { value: number }) => b.value - a.value;
  greeting() { const h = new Date().getHours(); return h < 12 ? 'Bonjour' : h < 18 ? 'Bon après-midi' : 'Bonsoir'; }
  firstName() { return (this.auth.user()?.fullName ?? '').split(' ')[0]; }
  periodLabel() { return this.days === 7 ? 'cette semaine' : this.days === 30 ? 'ce mois-ci' : 'sur le trimestre'; }
}
