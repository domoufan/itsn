import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { Page, STATUS_LABELS, TicketSummary } from '../../core/models';
import { DueInPipe, PriorityLabelPipe, StatusLabelPipe } from '../../core/pipes';
import { EmptyComponent } from '../../shared/ui';

@Component({
  selector: 'app-ticket-list',
  imports: [DatePipe, FormsModule, RouterLink, MatTableModule, MatPaginatorModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatButtonModule, MatIconModule, MatCheckboxModule, StatusLabelPipe, PriorityLabelPipe, DueInPipe, EmptyComponent],
  styles: ['.ref { color: var(--itsn-green-700); }'],
  template: `
    <div class="page">
      <div class="page-header">
        <div><h1>Tickets</h1><p class="subtitle">{{ page().totalElements }} ticket(s) correspondant aux filtres</p></div>
        <a mat-flat-button color="primary" routerLink="/tickets/new"><mat-icon>add</mat-icon> Nouveau ticket</a>
      </div>
      <div class="toolbar-row">
        <mat-form-field appearance="outline" subscriptSizing="dynamic" style="min-width:260px">
          <mat-label>Recherche (référence, titre, description)</mat-label>
          <input matInput [(ngModel)]="search" (keyup.enter)="reload()" />
        </mat-form-field>
        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>État</mat-label>
          <mat-select [(ngModel)]="status" (selectionChange)="reload()">
            <mat-option value="">Tous</mat-option>
            @for (s of statuses; track s) { <mat-option [value]="s">{{ s | statusLabel }}</mat-option> }
          </mat-select>
        </mat-form-field>
        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>Priorité</mat-label>
          <mat-select [(ngModel)]="priority" (selectionChange)="reload()">
            <mat-option value="">Toutes</mat-option>
            @for (p of ['P1','P2','P3','P4']; track p) { <mat-option [value]="p">{{ p | priorityLabel }}</mat-option> }
          </mat-select>
        </mat-form-field>
        <mat-checkbox [(ngModel)]="openOnly" (change)="reload()">Ouverts seulement</mat-checkbox>
        <mat-checkbox [(ngModel)]="slaBreached" (change)="reload()">SLA dépassé</mat-checkbox>
        @if (auth.role() === 'TECHNICIAN') { <mat-checkbox [(ngModel)]="mine" (change)="reload()">Mes tickets</mat-checkbox> }
        <button mat-stroked-button (click)="reload()"><mat-icon>search</mat-icon> Filtrer</button>
      </div>

      <table mat-table [dataSource]="page().content">
        <ng-container matColumnDef="reference"><th mat-header-cell *matHeaderCellDef>Référence</th><td mat-cell *matCellDef="let t" class="mono ref">{{ t.reference }}</td></ng-container>
        <ng-container matColumnDef="title"><th mat-header-cell *matHeaderCellDef>Titre</th><td mat-cell *matCellDef="let t">{{ t.title }}<div class="small muted">{{ t.category?.name }} · {{ t.site?.name }}</div></td></ng-container>
        <ng-container matColumnDef="priority"><th mat-header-cell *matHeaderCellDef>Priorité</th><td mat-cell *matCellDef="let t"><span class="badge prio-{{ t.priority }}">{{ t.priority }}</span></td></ng-container>
        <ng-container matColumnDef="status"><th mat-header-cell *matHeaderCellDef>État</th><td mat-cell *matCellDef="let t"><span class="badge st-{{ t.status }}">{{ t.status | statusLabel }}</span></td></ng-container>
        <ng-container matColumnDef="requester"><th mat-header-cell *matHeaderCellDef>Demandeur</th><td mat-cell *matCellDef="let t">{{ t.requester?.fullName }}</td></ng-container>
        <ng-container matColumnDef="assignee"><th mat-header-cell *matHeaderCellDef>Technicien</th><td mat-cell *matCellDef="let t">{{ t.assignee?.fullName || '—' }}</td></ng-container>
        <ng-container matColumnDef="createdAt"><th mat-header-cell *matHeaderCellDef>Créé le</th><td mat-cell *matCellDef="let t">{{ t.createdAt | date:'dd/MM/yy HH:mm' }}</td></ng-container>
        <ng-container matColumnDef="due"><th mat-header-cell *matHeaderCellDef>Échéance résolution</th>
          <td mat-cell *matCellDef="let t" [class.sla-breach]="t.slaResolutionBreached || t.slaResponseBreached">
            @if (t.status === 'RESOLVED' || t.status === 'CLOSED' || t.status === 'CANCELLED') { — } @else { {{ t.resolutionDueAt | dueIn }} }
            @if (t.slaResponseBreached || t.slaResolutionBreached) { <mat-icon inline>warning</mat-icon> }
          </td></ng-container>
        <tr mat-header-row *matHeaderRowDef="columns"></tr>
        <tr mat-row *matRowDef="let t; columns: columns" class="clickable" (click)="open(t)"></tr>
        <tr class="mat-row" *matNoDataRow><td class="mat-cell" [attr.colspan]="columns.length"><itsn-empty title="Aucun ticket" text="Aucun ticket ne correspond aux critères choisis." /></td></tr>
      </table>
      <mat-paginator [length]="page().totalElements" [pageSize]="pageSize" [pageIndex]="page().number" [pageSizeOptions]="[10, 20, 50]" (page)="onPage($event)" />
    </div>
  `,
})
export class TicketListComponent {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);
  readonly auth = inject(AuthService);

  readonly statuses = Object.keys(STATUS_LABELS);
  readonly columns = ['reference', 'title', 'priority', 'status', 'requester', 'assignee', 'createdAt', 'due'];
  readonly page = signal<Page<TicketSummary>>({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 20 });

  search = ''; status = ''; priority = ''; openOnly = true; slaBreached = false; mine = false; pageSize = 20; pageIndex = 0;

  constructor() { this.reload(); }

  reload() {
    this.api.tickets({
      search: this.search, status: this.status, priority: this.priority, openOnly: this.openOnly || undefined, slaBreached: this.slaBreached || undefined,
      assigneeId: this.mine ? this.auth.user()?.id : undefined, page: this.pageIndex, size: this.pageSize,
    }).subscribe((p) => this.page.set(p));
  }

  onPage(e: PageEvent) { this.pageIndex = e.pageIndex; this.pageSize = e.pageSize; this.reload(); }
  open(t: TicketSummary) { this.router.navigate(['/tickets', t.id]); }
}
