import { Component, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatChipsModule } from '@angular/material/chips';
import { ApiService } from '../../core/api.service';
import { NotifyService } from '../../core/notify.service';
import { AiStatus, Suggestion } from '../../core/models';

/**
 * File de revue humaine du pipeline agentique (exigences EF-12, EF-13).
 * Chaque proposition est comparée aux valeurs actuelles du ticket ; le responsable accepte, ajuste ou rejette.
 */
@Component({
  selector: 'app-ai-review',
  imports: [DatePipe, DecimalPipe, FormsModule, RouterLink, MatCardModule, MatButtonModule, MatIconModule, MatCheckboxModule, MatFormFieldModule, MatInputModule, MatChipsModule],
  template: `
    <div class="page">
      <div class="page-header"><div><h1>Propositions de l'agent IA</h1><p class="subtitle">Propositions de l'agent de triage en attente de votre décision</p></div></div>
      @if (status(); as s) {
        <mat-card class="lift" style="margin-bottom:16px"><mat-card-content class="toolbar-row">
          <mat-chip-set>
            <mat-chip [highlighted]="s.enabled" [color]="s.enabled ? 'primary' : undefined">{{ s.enabled ? 'API IA active (' + s.model + ')' : 'IA désactivée — simulation locale' }}</mat-chip>
            <mat-chip>Budget du jour : {{ s.spentTodayUsd | number:'1.4-4' }} / {{ s.dailyBudgetUsd | number:'1.2-2' }} USD</mat-chip>
            <mat-chip>{{ s.pendingSuggestions }} en attente</mat-chip>
            @if (s.fallbackActive) { <mat-chip color="warn" highlighted>Mode de repli heuristique actif</mat-chip> }
          </mat-chip-set>
          <span class="spacer"></span>
          <button mat-stroked-button (click)="load()"><mat-icon>refresh</mat-icon> Actualiser</button>
        </mat-card-content></mat-card>
      }
      @for (s of items(); track s.id) {
        <mat-card class="lift" style="margin-bottom:16px">
          <mat-card-header>
            <mat-card-title><a [routerLink]="['/tickets', s.ticketId]" class="mono">{{ s.ticketReference }}</a> — {{ s.ticketTitle }}</mat-card-title>
            <mat-card-subtitle>{{ s.createdAt | date:'dd/MM HH:mm' }} · {{ s.model }} · {{ s.toolCalls }} outil(s) · {{ s.latencyMs }} ms · {{ s.inputTokens + s.outputTokens }} jetons · {{ s.estimatedCostUsd | number:'1.4-4' }} USD</mat-card-subtitle>
          </mat-card-header>
          <mat-card-content>
            <p><strong>Résumé :</strong> {{ s.summary }}</p>
            <table class="cmp">
              <tr><th></th><th>Actuel</th><th>Proposé</th><th>Appliquer</th></tr>
              <tr><td>Catégorie</td><td>{{ s.currentCategoryCode }}</td><td [class.diff]="s.currentCategoryCode !== s.suggestedCategoryCode"><strong>{{ s.suggestedCategoryCode }}</strong> ({{ s.suggestedCategoryName }})</td><td><mat-checkbox [(ngModel)]="choice[s.id].category" /></td></tr>
              <tr><td>Priorité</td><td><span class="badge prio-{{ s.currentPriority }}">{{ s.currentPriority }}</span></td><td [class.diff]="s.currentPriority !== s.suggestedPriority"><span class="badge prio-{{ s.suggestedPriority }}">{{ s.suggestedPriority }}</span> <span class="small muted">(impact {{ s.suggestedImpact }}, urgence {{ s.suggestedUrgency }})</span></td><td><mat-checkbox [(ngModel)]="choice[s.id].priority" /></td></tr>
              <tr><td>Technicien</td><td>{{ s.currentAssigneeName || '—' }}</td><td [class.diff]="s.currentAssigneeName !== s.suggestedAssigneeName"><strong>{{ s.suggestedAssigneeName || 'aucune proposition' }}</strong></td><td><mat-checkbox [(ngModel)]="choice[s.id].assignee" [disabled]="!s.suggestedAssigneeId" /></td></tr>
            </table>
            <p class="small muted pre"><strong>Justification de l'agent :</strong> {{ s.rationale }}</p>
            @if (s.relatedArticleIds) { <p class="small">Articles suggérés : {{ s.relatedArticleIds }}</p> }
            <mat-form-field class="full" appearance="outline"><mat-label>Commentaire de revue (optionnel)</mat-label><input matInput [(ngModel)]="choice[s.id].comment" /></mat-form-field>
          </mat-card-content>
          <mat-card-actions>
            <button mat-flat-button color="primary" (click)="review(s, 'ACCEPT')"><mat-icon>done_all</mat-icon> Tout accepter</button>
            <button mat-stroked-button (click)="review(s, 'PARTIAL')"><mat-icon>rule</mat-icon> Appliquer la sélection</button>
            <button mat-stroked-button color="warn" (click)="review(s, 'REJECT')"><mat-icon>block</mat-icon> Rejeter</button>
          </mat-card-actions>
        </mat-card>
      } @empty { <p class="muted">Aucune proposition en attente. Lancez « Triage IA » depuis la fiche d'un ticket.</p> }
    </div>
  `,
  styles: [`
    table.cmp { border-collapse: collapse; margin: 8px 0; }
    table.cmp th, table.cmp td { padding: 6px 14px; text-align: left; border-bottom: 1px solid #e5e7eb; }
    td.diff { background: #fef9c3; }
  `],
})
export class AiReviewComponent {
  private readonly api = inject(ApiService);
  private readonly notify = inject(NotifyService);
  readonly items = signal<Suggestion[]>([]);
  readonly status = signal<AiStatus | null>(null);
  choice: Record<number, { category: boolean; priority: boolean; assignee: boolean; comment: string }> = {};
  constructor() { this.load(); }
  load() {
    this.api.aiStatus().subscribe((s) => this.status.set(s));
    this.api.pendingSuggestions().subscribe((list) => {
      list.forEach((s) => (this.choice[s.id] ??= { category: true, priority: true, assignee: !!s.suggestedAssigneeId, comment: '' }));
      this.items.set(list);
    });
  }
  review(s: Suggestion, decision: 'ACCEPT' | 'PARTIAL' | 'REJECT') {
    const c = this.choice[s.id];
    this.api.reviewSuggestion(s.id, { decision, applyCategory: c.category, applyPriority: c.priority, applyAssignee: c.assignee, comment: c.comment || undefined })
      .subscribe({ next: () => { this.notify.success('Décision enregistrée'); this.load(); }, error: (e) => this.notify.error(e) });
  }
}
