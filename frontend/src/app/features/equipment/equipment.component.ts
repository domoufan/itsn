import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { ApiService } from '../../core/api.service';
import { NotifyService } from '../../core/notify.service';
import { Equipment, Site } from '../../core/models';

/** Inventaire des équipements par site client (exigence EF-11). */
@Component({
  selector: 'app-equipment',
  imports: [FormsModule, MatCardModule, MatButtonModule, MatIconModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatTableModule],
  template: `
    <div class="page">
      <div class="page-header"><div><h1>Équipements</h1><p class="subtitle">Inventaire des équipements rattachés aux sites clients</p></div>
        <div class="toolbar-row">
          <mat-form-field appearance="outline" subscriptSizing="dynamic"><mat-label>Site</mat-label>
            <mat-select [(ngModel)]="siteId" (selectionChange)="load()"><mat-option [value]="null">Tous</mat-option>@for (s of sites(); track s.id) { <mat-option [value]="s.id">{{ s.name }}</mat-option> }</mat-select></mat-form-field>
          <button mat-flat-button color="primary" (click)="edit({ type: 'LAPTOP', status: 'IN_SERVICE' })"><mat-icon>add</mat-icon> Ajouter</button>
        </div>
      </div>
      @if (draft(); as d) {
        <mat-card class="lift" style="margin-bottom:16px"><mat-card-content>
          <div class="form-grid">
            <mat-form-field appearance="outline"><mat-label>N° inventaire</mat-label><input matInput [(ngModel)]="d.assetTag" /></mat-form-field>
            <mat-form-field appearance="outline"><mat-label>Type</mat-label><mat-select [(ngModel)]="d.type">@for (t of types; track t) { <mat-option [value]="t">{{ t }}</mat-option> }</mat-select></mat-form-field>
            <mat-form-field appearance="outline"><mat-label>Marque</mat-label><input matInput [(ngModel)]="d.brand" /></mat-form-field>
            <mat-form-field appearance="outline"><mat-label>Modèle</mat-label><input matInput [(ngModel)]="d.model" /></mat-form-field>
            <mat-form-field appearance="outline"><mat-label>N° de série</mat-label><input matInput [(ngModel)]="d.serialNumber" /></mat-form-field>
            <mat-form-field appearance="outline"><mat-label>Site</mat-label><mat-select [(ngModel)]="d.siteId">@for (s of sites(); track s.id) { <mat-option [value]="s.id">{{ s.name }}</mat-option> }</mat-select></mat-form-field>
            <mat-form-field appearance="outline"><mat-label>État</mat-label><mat-select [(ngModel)]="d.status"><mat-option value="IN_SERVICE">En service</mat-option><mat-option value="IN_REPAIR">En réparation</mat-option><mat-option value="RETIRED">Réformé</mat-option></mat-select></mat-form-field>
            <mat-form-field appearance="outline"><mat-label>Fin de garantie</mat-label><input matInput type="date" [(ngModel)]="d.warrantyEnd" /></mat-form-field>
          </div>
          <button mat-flat-button color="primary" (click)="save(d)">Enregistrer</button> <button mat-button (click)="draft.set(null)">Annuler</button>
        </mat-card-content></mat-card>
      }
      <table mat-table [dataSource]="items()">
        <ng-container matColumnDef="assetTag"><th mat-header-cell *matHeaderCellDef>Inventaire</th><td mat-cell *matCellDef="let e" class="mono">{{ e.assetTag }}</td></ng-container>
        <ng-container matColumnDef="type"><th mat-header-cell *matHeaderCellDef>Type</th><td mat-cell *matCellDef="let e">{{ e.type }}</td></ng-container>
        <ng-container matColumnDef="model"><th mat-header-cell *matHeaderCellDef>Marque / modèle</th><td mat-cell *matCellDef="let e">{{ e.brand }} {{ e.model }}<div class="small muted">{{ e.serialNumber }}</div></td></ng-container>
        <ng-container matColumnDef="site"><th mat-header-cell *matHeaderCellDef>Site</th><td mat-cell *matCellDef="let e">{{ e.siteName }}</td></ng-container>
        <ng-container matColumnDef="status"><th mat-header-cell *matHeaderCellDef>État</th><td mat-cell *matCellDef="let e">{{ e.status }}</td></ng-container>
        <ng-container matColumnDef="warranty"><th mat-header-cell *matHeaderCellDef>Garantie</th><td mat-cell *matCellDef="let e" [class.sla-breach]="expired(e)">{{ e.warrantyEnd || '—' }}</td></ng-container>
        <ng-container matColumnDef="actions"><th mat-header-cell *matHeaderCellDef></th><td mat-cell *matCellDef="let e"><button mat-icon-button (click)="edit(e)"><mat-icon>edit</mat-icon></button></td></ng-container>
        <tr mat-header-row *matHeaderRowDef="cols"></tr><tr mat-row *matRowDef="let r; columns: cols"></tr>
      </table>
    </div>
  `,
})
export class EquipmentComponent {
  private readonly api = inject(ApiService);
  private readonly notify = inject(NotifyService);
  readonly cols = ['assetTag', 'type', 'model', 'site', 'status', 'warranty', 'actions'];
  readonly types = ['LAPTOP', 'DESKTOP', 'PRINTER', 'SERVER', 'SWITCH', 'ROUTER', 'ACCESS_POINT', 'UPS', 'OTHER'];
  readonly items = signal<Equipment[]>([]);
  readonly sites = signal<Site[]>([]);
  readonly draft = signal<Partial<Equipment> | null>(null);
  siteId: number | null = null;
  constructor() { this.api.sites().subscribe((s) => this.sites.set(s)); this.load(); }
  load() { this.api.equipment(this.siteId ?? undefined).subscribe((e) => this.items.set(e)); }
  edit(e: Partial<Equipment>) { this.draft.set({ ...e }); }
  save(d: Partial<Equipment>) { this.api.saveEquipment(d).subscribe({ next: () => { this.notify.success('Équipement enregistré'); this.draft.set(null); this.load(); }, error: (e) => this.notify.error(e) }); }
  expired(e: Equipment) { return !!e.warrantyEnd && new Date(e.warrantyEnd) < new Date(); }
}
