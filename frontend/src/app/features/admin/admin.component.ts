import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatTabsModule } from '@angular/material/tabs';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { ApiService } from '../../core/api.service';
import { NotifyService } from '../../core/notify.service';
import { ROLE_LABELS, Site, SlaPolicy, UserAdmin } from '../../core/models';
import { RoleLabelPipe } from '../../core/pipes';

/** Administration : comptes, sites clients, politiques SLA (exigences EF-01, EF-06, ENF-02). */
@Component({
  selector: 'app-admin',
  imports: [DatePipe, FormsModule, MatCardModule, MatButtonModule, MatIconModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatTableModule, MatTabsModule, MatCheckboxModule, RoleLabelPipe],
  template: `
    <div class="page">
      <div class="page-header"><div><h1>Administration</h1><p class="subtitle">Comptes, sites clients et politiques de service</p></div></div>
      <mat-tab-group>
        <mat-tab label="Utilisateurs">
          <div style="padding:16px 0">
            <button mat-flat-button color="primary" (click)="userDraft.set({ role: 'REQUESTER', active: true })"><mat-icon>person_add</mat-icon> Nouveau compte</button>
            @if (userDraft(); as d) {
              <mat-card style="margin:16px 0"><mat-card-content>
                <div class="form-grid">
                  <mat-form-field appearance="outline"><mat-label>Nom complet</mat-label><input matInput [(ngModel)]="d.fullName" /></mat-form-field>
                  <mat-form-field appearance="outline"><mat-label>E-mail</mat-label><input matInput type="email" [(ngModel)]="d.email" /></mat-form-field>
                  <mat-form-field appearance="outline"><mat-label>Rôle</mat-label><mat-select [(ngModel)]="d.role">@for (r of roles; track r) { <mat-option [value]="r">{{ r | roleLabel }}</mat-option> }</mat-select></mat-form-field>
                  <mat-form-field appearance="outline"><mat-label>Site</mat-label><mat-select [(ngModel)]="d.siteId"><mat-option [value]="null">—</mat-option>@for (s of sites(); track s.id) { <mat-option [value]="s.id">{{ s.name }}</mat-option> }</mat-select></mat-form-field>
                  <mat-form-field appearance="outline"><mat-label>Téléphone</mat-label><input matInput [(ngModel)]="d.phone" /></mat-form-field>
                  <mat-form-field appearance="outline"><mat-label>Compétences (codes catégories, ex. NETWORK,SERVER)</mat-label><input matInput [(ngModel)]="d.skills" /></mat-form-field>
                  <mat-form-field appearance="outline"><mat-label>{{ d.id ? 'Nouveau mot de passe (laisser vide pour conserver)' : 'Mot de passe initial' }}</mat-label><input matInput type="password" [(ngModel)]="password" autocomplete="new-password" /></mat-form-field>
                </div>
                <mat-checkbox [(ngModel)]="d.active">Compte actif</mat-checkbox>
                <div><button mat-flat-button color="primary" (click)="saveUser(d)">Enregistrer</button> <button mat-button (click)="userDraft.set(null)">Annuler</button></div>
              </mat-card-content></mat-card>
            }
            <table mat-table [dataSource]="users()" style="margin-top:12px">
              <ng-container matColumnDef="name"><th mat-header-cell *matHeaderCellDef>Nom</th><td mat-cell *matCellDef="let u">{{ u.fullName }}<div class="small muted">{{ u.email }}</div></td></ng-container>
              <ng-container matColumnDef="role"><th mat-header-cell *matHeaderCellDef>Rôle</th><td mat-cell *matCellDef="let u">{{ u.role | roleLabel }}</td></ng-container>
              <ng-container matColumnDef="site"><th mat-header-cell *matHeaderCellDef>Site</th><td mat-cell *matCellDef="let u">{{ u.siteName || '—' }}</td></ng-container>
              <ng-container matColumnDef="skills"><th mat-header-cell *matHeaderCellDef>Compétences</th><td mat-cell *matCellDef="let u" class="small">{{ u.skills || '—' }}</td></ng-container>
              <ng-container matColumnDef="login"><th mat-header-cell *matHeaderCellDef>Dernière connexion</th><td mat-cell *matCellDef="let u" class="small">{{ u.lastLoginAt ? (u.lastLoginAt | date:'dd/MM/yy HH:mm') : 'jamais' }}</td></ng-container>
              <ng-container matColumnDef="active"><th mat-header-cell *matHeaderCellDef>Actif</th><td mat-cell *matCellDef="let u">{{ u.active ? 'oui' : 'non' }}</td></ng-container>
              <ng-container matColumnDef="actions"><th mat-header-cell *matHeaderCellDef></th><td mat-cell *matCellDef="let u">
                <button mat-icon-button (click)="userDraft.set({ ...u })"><mat-icon>edit</mat-icon></button>
                @if (u.active) { <button mat-icon-button color="warn" (click)="deactivate(u)"><mat-icon>person_off</mat-icon></button> }</td></ng-container>
              <tr mat-header-row *matHeaderRowDef="userCols"></tr><tr mat-row *matRowDef="let r; columns: userCols"></tr>
            </table>
          </div>
        </mat-tab>
        <mat-tab label="Sites clients">
          <div style="padding:16px 0">
            <button mat-flat-button color="primary" (click)="siteDraft.set({ active: true })"><mat-icon>add_business</mat-icon> Nouveau site</button>
            @if (siteDraft(); as d) {
              <mat-card style="margin:16px 0"><mat-card-content><div class="form-grid">
                <mat-form-field appearance="outline"><mat-label>Nom du site</mat-label><input matInput [(ngModel)]="d.name" /></mat-form-field>
                <mat-form-field appearance="outline"><mat-label>Client</mat-label><input matInput [(ngModel)]="d.clientName" /></mat-form-field>
                <mat-form-field appearance="outline"><mat-label>Adresse</mat-label><input matInput [(ngModel)]="d.address" /></mat-form-field>
                <mat-form-field appearance="outline"><mat-label>Ville</mat-label><input matInput [(ngModel)]="d.city" /></mat-form-field>
                <mat-form-field appearance="outline"><mat-label>Téléphone</mat-label><input matInput [(ngModel)]="d.contactPhone" /></mat-form-field>
              </div><button mat-flat-button color="primary" (click)="saveSite(d)">Enregistrer</button> <button mat-button (click)="siteDraft.set(null)">Annuler</button></mat-card-content></mat-card>
            }
            <table mat-table [dataSource]="sites()" style="margin-top:12px">
              <ng-container matColumnDef="name"><th mat-header-cell *matHeaderCellDef>Site</th><td mat-cell *matCellDef="let s">{{ s.name }}</td></ng-container>
              <ng-container matColumnDef="client"><th mat-header-cell *matHeaderCellDef>Client</th><td mat-cell *matCellDef="let s">{{ s.clientName }}</td></ng-container>
              <ng-container matColumnDef="city"><th mat-header-cell *matHeaderCellDef>Ville</th><td mat-cell *matCellDef="let s">{{ s.city }}</td></ng-container>
              <ng-container matColumnDef="phone"><th mat-header-cell *matHeaderCellDef>Contact</th><td mat-cell *matCellDef="let s">{{ s.contactPhone }}</td></ng-container>
              <ng-container matColumnDef="actions"><th mat-header-cell *matHeaderCellDef></th><td mat-cell *matCellDef="let s"><button mat-icon-button (click)="siteDraft.set({ ...s })"><mat-icon>edit</mat-icon></button></td></ng-container>
              <tr mat-header-row *matHeaderRowDef="siteCols"></tr><tr mat-row *matRowDef="let r; columns: siteCols"></tr>
            </table>
          </div>
        </mat-tab>
        <mat-tab label="Politiques SLA">
          <div style="padding:16px 0" class="grid grid-4 stagger">
            @for (p of sla(); track p.priority) {
              <mat-card class="lift"><mat-card-header><mat-card-title><span class="badge prio-{{ p.priority }}">{{ p.priority }}</span></mat-card-title></mat-card-header>
                <mat-card-content>
                  <mat-form-field class="full" appearance="outline"><mat-label>Libellé</mat-label><input matInput [(ngModel)]="p.label" /></mat-form-field>
                  <mat-form-field class="full" appearance="outline"><mat-label>Prise en compte (minutes)</mat-label><input matInput type="number" [(ngModel)]="p.responseMinutes" /></mat-form-field>
                  <mat-form-field class="full" appearance="outline"><mat-label>Résolution (minutes)</mat-label><input matInput type="number" [(ngModel)]="p.resolutionMinutes" /></mat-form-field>
                  <button mat-stroked-button (click)="saveSla(p)">Enregistrer</button>
                </mat-card-content></mat-card>
            }
          </div>
        </mat-tab>
      </mat-tab-group>
    </div>
  `,
})
export class AdminComponent {
  private readonly api = inject(ApiService);
  private readonly notify = inject(NotifyService);
  readonly roles = Object.keys(ROLE_LABELS);
  readonly userCols = ['name', 'role', 'site', 'skills', 'login', 'active', 'actions'];
  readonly siteCols = ['name', 'client', 'city', 'phone', 'actions'];
  readonly users = signal<UserAdmin[]>([]);
  readonly sites = signal<Site[]>([]);
  readonly sla = signal<SlaPolicy[]>([]);
  readonly userDraft = signal<Partial<UserAdmin> | null>(null);
  readonly siteDraft = signal<Partial<Site> | null>(null);
  password = '';
  constructor() { this.load(); }
  load() {
    this.api.users().subscribe((u) => this.users.set(u));
    this.api.sites().subscribe((s) => this.sites.set(s));
    this.api.slaPolicies().subscribe((p) => this.sla.set(p));
  }
  saveUser(d: Partial<UserAdmin>) {
    this.api.saveUser({ ...d, password: this.password || undefined }).subscribe({ next: () => { this.notify.success('Compte enregistré'); this.userDraft.set(null); this.password = ''; this.load(); }, error: (e) => this.notify.error(e) });
  }
  deactivate(u: UserAdmin) { if (confirm(`Désactiver le compte de ${u.fullName} ?`)) this.api.deactivateUser(u.id).subscribe({ next: () => this.load(), error: (e) => this.notify.error(e) }); }
  saveSite(d: Partial<Site>) { this.api.saveSite(d).subscribe({ next: () => { this.notify.success('Site enregistré'); this.siteDraft.set(null); this.load(); }, error: (e) => this.notify.error(e) }); }
  saveSla(p: SlaPolicy) { this.api.updateSla(p).subscribe({ next: () => this.notify.success('Politique SLA mise à jour'), error: (e) => this.notify.error(e) }); }
}
