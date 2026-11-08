import { Component, inject, input, effect, signal, computed } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatTabsModule } from '@angular/material/tabs';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { NotifyService } from '../../core/notify.service';
import { Comment, HistoryEntry, Intervention, SCALE_LABELS, Suggestion, Ticket, TicketStatus, UserAdmin } from '../../core/models';
import { DueInPipe, PriorityLabelPipe, StatusLabelPipe } from '../../core/pipes';

/** Fiche ticket : informations, actions selon le rôle, commentaires, historique, interventions et triage IA. */
@Component({
  selector: 'app-ticket-detail',
  imports: [DatePipe, FormsModule, RouterLink, MatCardModule, MatButtonModule, MatIconModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatCheckboxModule, MatTabsModule, MatDividerModule, MatTooltipModule, StatusLabelPipe, PriorityLabelPipe, DueInPipe],
  template: `
    @if (ticket(); as t) {
    <div class="page">
      <div class="page-header">
        <div>
          <a mat-button routerLink="/tickets"><mat-icon>arrow_back</mat-icon> Tickets</a>
          <h1><span class="mono">{{ t.reference }}</span> — {{ t.title }}</h1>
          <div class="toolbar-row">
            <span class="badge prio-{{ t.priority }}">{{ t.priority | priorityLabel }}</span>
            <span class="badge st-{{ t.status }}">{{ t.status | statusLabel }}</span>
            <span class="muted small">{{ t.type === 'INCIDENT' ? 'Incident' : 'Demande' }} · {{ t.category?.name }} · créé le {{ t.createdAt | date:'dd/MM/yyyy HH:mm' }} par {{ t.requester.fullName }}</span>
          </div>
        </div>
      </div>

      <div class="grid grid-2">
        <mat-card>
          <mat-card-header><mat-card-title>Description</mat-card-title></mat-card-header>
          <mat-card-content>
            <p class="pre">{{ t.description }}</p>
            <mat-divider />
            <div class="grid grid-2 small" style="margin-top:12px">
              <div><span class="muted">Site :</span> {{ t.site?.name || '—' }} <span class="muted">{{ t.site?.clientName }}</span></div>
              <div><span class="muted">Équipement :</span> {{ t.equipment ? t.equipment.assetTag + ' (' + t.equipment.brand + ' ' + t.equipment.model + ')' : '—' }}</div>
              <div><span class="muted">Impact :</span> {{ scale[t.impact] }}</div>
              <div><span class="muted">Urgence :</span> {{ scale[t.urgency] }}</div>
              <div><span class="muted">Technicien :</span> {{ t.assignee?.fullName || 'non affecté' }}</div>
              <div><span class="muted">Satisfaction :</span> {{ t.satisfactionScore ? t.satisfactionScore + '/5' : '—' }}</div>
            </div>
            @if (t.resolutionSummary) { <mat-divider /><h4>Résolution</h4><p class="pre">{{ t.resolutionSummary }}</p> }
          </mat-card-content>
        </mat-card>

        <mat-card>
          <mat-card-header><mat-card-title>Engagements de service (SLA)</mat-card-title></mat-card-header>
          <mat-card-content class="small">
            <p>Prise en compte attendue : <strong>{{ t.responseDueAt | date:'dd/MM HH:mm' }}</strong>
              @if (t.firstResponseAt) { — réalisée le {{ t.firstResponseAt | date:'dd/MM HH:mm' }} <span [class.sla-breach]="t.slaResponseBreached">{{ t.slaResponseBreached ? '(dépassée)' : '(respectée)' }}</span> }
              @else { — <span [class.sla-breach]="t.slaResponseBreached">{{ t.responseDueAt | dueIn }}</span> }
            </p>
            <p>Résolution attendue : <strong>{{ t.resolutionDueAt | date:'dd/MM HH:mm' }}</strong>
              @if (t.resolvedAt) { — réalisée le {{ t.resolvedAt | date:'dd/MM HH:mm' }} <span [class.sla-breach]="t.slaResolutionBreached">{{ t.slaResolutionBreached ? '(dépassée)' : '(respectée)' }}</span> }
              @else { — <span [class.sla-breach]="t.slaResolutionBreached">{{ t.resolutionDueAt | dueIn }}</span> }
            </p>
            <mat-divider />
            <h4>Actions</h4>
            <div class="toolbar-row">
              @if (auth.isManager() && canAssign()) {
                <mat-form-field appearance="outline" subscriptSizing="dynamic" style="min-width:260px">
                  <mat-label>Affecter à</mat-label>
                  <mat-select [(ngModel)]="assigneeId">
                    @for (u of technicians(); track u.id) { <mat-option [value]="u.id">{{ u.fullName }} — {{ u.openTickets }} en cours · {{ u.skills }}</mat-option> }
                  </mat-select>
                </mat-form-field>
                <button mat-stroked-button (click)="assign()" [disabled]="!assigneeId"><mat-icon>person_add</mat-icon> Affecter</button>
              }
              @for (s of visibleTransitions(); track s) {
                <button mat-stroked-button (click)="askStatus(s)"><mat-icon>{{ icon(s) }}</mat-icon> {{ label(s) }}</button>
              }
              @if (auth.isStaff() && canAssign()) {
                <button mat-stroked-button color="accent" (click)="triage()" matTooltip="Analyse par l'agent IA ; la proposition devra être validée par le responsable"><mat-icon>smart_toy</mat-icon> Triage IA</button>
              }
              @if (isRequester() && (t.status === 'RESOLVED' || t.status === 'CLOSED') && !t.satisfactionScore) {
                <mat-form-field appearance="outline" subscriptSizing="dynamic"><mat-label>Votre évaluation</mat-label>
                  <mat-select [(ngModel)]="score">@for (k of [5,4,3,2,1]; track k) { <mat-option [value]="k">{{ k }} / 5</mat-option> }</mat-select></mat-form-field>
                <button mat-stroked-button (click)="rate()" [disabled]="!score">Évaluer</button>
              }
            </div>
            @if (pendingStatus(); as s) {
              <div style="margin-top:8px">
                <h4>{{ label(s) }}</h4>
                @if (s === 'RESOLVED') { <mat-form-field class="full" appearance="outline"><mat-label>Résumé de la résolution (obligatoire)</mat-label><textarea matInput rows="3" [(ngModel)]="resolutionSummary"></textarea></mat-form-field> }
                <mat-form-field class="full" appearance="outline"><mat-label>Commentaire (optionnel)</mat-label><textarea matInput rows="2" [(ngModel)]="statusComment"></textarea></mat-form-field>
                <button mat-flat-button color="primary" (click)="confirmStatus()">Confirmer</button>
                <button mat-button (click)="pendingStatus.set(null)">Annuler</button>
              </div>
            }
          </mat-card-content>
        </mat-card>
      </div>

      <mat-card style="margin-top:16px">
        <mat-tab-group>
          <mat-tab label="Commentaires ({{ comments().length }})">
            <div style="padding:16px">
              @for (c of comments(); track c.id) {
                <div class="comment" [class.internal]="c.internal">
                  <div class="small muted"><strong>{{ c.author.fullName }}</strong> · {{ c.createdAt | date:'dd/MM/yyyy HH:mm' }} @if (c.internal) { · <mat-icon inline>lock</mat-icon> note interne }</div>
                  <div class="pre">{{ c.content }}</div>
                </div>
              } @empty { <p class="muted">Aucun commentaire.</p> }
              @if (t.status !== 'CLOSED' && t.status !== 'CANCELLED') {
                <mat-form-field class="full" appearance="outline"><mat-label>Ajouter un commentaire</mat-label><textarea matInput rows="2" [(ngModel)]="newComment"></textarea></mat-form-field>
                <div class="toolbar-row">
                  @if (auth.isStaff()) { <mat-checkbox [(ngModel)]="internalComment">Note interne (invisible du demandeur)</mat-checkbox> }
                  <button mat-flat-button color="primary" (click)="comment()" [disabled]="!newComment.trim()">Publier</button>
                </div>
              }
            </div>
          </mat-tab>
          @if (auth.isStaff()) {
          <mat-tab label="Interventions ({{ interventions().length }})">
            <div style="padding:16px">
              @for (i of interventions(); track i.id) {
                <div class="comment"><span class="badge st-{{ i.status }}">{{ i.status }}</span> {{ i.mode === 'ONSITE' ? 'Sur site' : 'À distance' }} · {{ i.technicianName }} · {{ i.scheduledStart | date:'dd/MM HH:mm' }} → {{ i.scheduledEnd | date:'HH:mm' }}
                  @if (i.report) { <div class="pre small">{{ i.report }}</div> }</div>
              } @empty { <p class="muted">Aucune intervention planifiée. <a routerLink="/planning">Ouvrir le planning</a></p> }
            </div>
          </mat-tab>
          <mat-tab label="Triage IA ({{ suggestions().length }})">
            <div style="padding:16px">
              @for (s of suggestions(); track s.id) {
                <div class="comment">
                  <div class="small muted"><span class="badge st-{{ s.status }}">{{ s.status }}</span> {{ s.createdAt | date:'dd/MM HH:mm' }} · modèle {{ s.model }} · {{ s.toolCalls }} appel(s) d'outil · {{ s.latencyMs }} ms · {{ s.estimatedCostUsd }} USD
                    @if (s.reviewedByName) { · revu par {{ s.reviewedByName }} }</div>
                  @if (s.errorMessage) { <p class="sla-breach">{{ s.errorMessage }}</p> } @else {
                    <p><strong>{{ s.summary }}</strong></p>
                    <p>Catégorie proposée : <strong>{{ s.suggestedCategoryName }}</strong> · Priorité proposée : <span class="badge prio-{{ s.suggestedPriority }}">{{ s.suggestedPriority }}</span> · Technicien proposé : <strong>{{ s.suggestedAssigneeName || '—' }}</strong></p>
                    <p class="small muted pre">{{ s.rationale }}</p>
                  }
                  @if (s.status === 'PENDING' && auth.isManager()) { <a mat-stroked-button routerLink="/ai">Traiter dans la file de revue</a> }
                </div>
              } @empty { <p class="muted">Aucune proposition. Le bouton « Triage IA » lance l'analyse ; rien n'est appliqué sans validation humaine.</p> }
            </div>
          </mat-tab>
          }
          <mat-tab label="Historique ({{ history().length }})">
            <div style="padding:16px" class="small">
              @for (h of history(); track h.id) {
                <div>{{ h.changedAt | date:'dd/MM/yyyy HH:mm' }} — <strong>{{ h.changedBy?.fullName || 'système' }}</strong> : {{ h.field }} {{ h.oldValue ? '« ' + h.oldValue + ' » → ' : '' }}« {{ h.newValue }} »</div>
              }
            </div>
          </mat-tab>
        </mat-tab-group>
      </mat-card>
    </div>
    }
  `,
  styles: [`
    .comment { padding: 10px 12px; border-left: 3px solid #cbd5e1; background: #fff; margin-bottom: 10px; border-radius: 4px; }
    .comment.internal { border-left-color: #f59e0b; background: #fffbeb; }
    h4 { margin: 12px 0 6px; }
  `],
})
export class TicketDetailComponent {
  readonly id = input.required<string>();
  private readonly api = inject(ApiService);
  private readonly notify = inject(NotifyService);
  readonly auth = inject(AuthService);

  readonly scale = SCALE_LABELS;
  readonly ticket = signal<Ticket | null>(null);
  readonly comments = signal<Comment[]>([]);
  readonly history = signal<HistoryEntry[]>([]);
  readonly interventions = signal<Intervention[]>([]);
  readonly suggestions = signal<Suggestion[]>([]);
  readonly technicians = signal<UserAdmin[]>([]);
  readonly pendingStatus = signal<TicketStatus | null>(null);

  assigneeId: number | null = null; statusComment = ''; resolutionSummary = ''; newComment = ''; internalComment = false; score: number | null = null;

  readonly isRequester = computed(() => this.ticket()?.requester.id === this.auth.user()?.id);
  readonly canAssign = computed(() => { const s = this.ticket()?.status; return !!s && !['RESOLVED', 'CLOSED', 'CANCELLED'].includes(s); });

  /** Transitions proposées par le serveur, restreintes aux actions pertinentes pour le rôle courant (le serveur reste l'autorité). */
  readonly visibleTransitions = computed<TicketStatus[]>(() => {
    const t = this.ticket(); if (!t) return [];
    const role = this.auth.role();
    const all = t.allowedTransitions.filter((s) => s !== 'ASSIGNED');
    if (role === 'REQUESTER') return all.filter((s) => (s === 'CLOSED' && t.status === 'RESOLVED') || (s === 'IN_PROGRESS' && t.status === 'RESOLVED') || (s === 'CANCELLED' && t.status === 'NEW'));
    if (role === 'TECHNICIAN') return t.assignee?.id === this.auth.user()?.id ? all.filter((s) => !['CLOSED', 'CANCELLED'].includes(s)) : [];
    return all;
  });

  constructor() {
    effect(() => { const id = Number(this.id()); if (id) this.load(id); });
  }

  load(id: number) {
    this.api.ticket(id).subscribe({ next: (t) => { this.ticket.set(t); this.assigneeId = t.assignee?.id ?? null; }, error: (e) => this.notify.error(e, 'Ticket inaccessible') });
    this.api.comments(id).subscribe((c) => this.comments.set(c));
    this.api.history(id).subscribe((h) => this.history.set(h));
    if (this.auth.isStaff()) {
      this.api.interventionsByTicket(id).subscribe((i) => this.interventions.set(i));
      this.api.suggestionsForTicket(id).subscribe((s) => this.suggestions.set(s));
      if (this.auth.isManager()) this.api.technicians().subscribe((u) => this.technicians.set(u));
    }
  }

  refresh() { this.load(Number(this.id())); }

  assign() {
    if (!this.assigneeId) return;
    this.api.assignTicket(Number(this.id()), this.assigneeId).subscribe({ next: () => { this.notify.success('Ticket affecté'); this.refresh(); }, error: (e) => this.notify.error(e) });
  }

  askStatus(s: TicketStatus) { this.pendingStatus.set(s); this.statusComment = ''; this.resolutionSummary = ''; }

  confirmStatus() {
    const s = this.pendingStatus(); if (!s) return;
    this.api.changeStatus(Number(this.id()), { status: s, comment: this.statusComment || undefined, resolutionSummary: this.resolutionSummary || undefined })
      .subscribe({ next: () => { this.notify.success('État mis à jour'); this.pendingStatus.set(null); this.refresh(); }, error: (e) => this.notify.error(e) });
  }

  comment() {
    this.api.addComment(Number(this.id()), this.newComment, this.internalComment).subscribe({ next: () => { this.newComment = ''; this.refresh(); }, error: (e) => this.notify.error(e) });
  }

  rate() {
    if (!this.score) return;
    this.api.rate(Number(this.id()), this.score).subscribe({ next: () => { this.notify.success('Merci pour votre évaluation'); this.refresh(); }, error: (e) => this.notify.error(e) });
  }

  triage() {
    this.api.triage(Number(this.id())).subscribe({
      next: (s) => { this.notify.success(s.status === 'FAILED' ? 'Analyse échouée : ' + s.errorMessage : 'Proposition générée (en attente de validation)'); this.refresh(); },
      error: (e) => this.notify.error(e),
    });
  }

  label(s: TicketStatus): string {
    return ({ IN_PROGRESS: this.ticket()?.status === 'RESOLVED' ? 'Rouvrir' : 'Prendre en charge', ON_HOLD: 'Mettre en attente', RESOLVED: 'Résoudre', CLOSED: 'Clôturer', CANCELLED: 'Annuler', NEW: 'Nouveau', ASSIGNED: 'Affecter' } as Record<TicketStatus, string>)[s];
  }
  icon(s: TicketStatus): string {
    return ({ IN_PROGRESS: 'play_arrow', ON_HOLD: 'pause', RESOLVED: 'check_circle', CLOSED: 'lock', CANCELLED: 'cancel', NEW: 'fiber_new', ASSIGNED: 'person' } as Record<TicketStatus, string>)[s];
  }
}
