import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { AuthService } from '../core/auth.service';
import { RoleLabelPipe } from '../core/pipes';

/** Gabarit principal : barre d'outils, navigation latérale filtrée selon le rôle. */
@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatToolbarModule, MatSidenavModule, MatListModule, MatIconModule, MatButtonModule, MatMenuModule, RoleLabelPipe],
  template: `
    <mat-sidenav-container class="shell">
      <mat-sidenav mode="side" opened class="sidenav">
        <div class="brand">
          <mat-icon>build_circle</mat-icon>
          <div><strong>IntervIT</strong><div class="small muted">SénéTech Services</div></div>
        </div>
        <mat-nav-list>
          @if (auth.isStaff()) {
            <a mat-list-item routerLink="/dashboard" routerLinkActive="active"><mat-icon matListItemIcon>dashboard</mat-icon><span matListItemTitle>Tableau de bord</span></a>
          }
          <a mat-list-item routerLink="/tickets" routerLinkActive="active"><mat-icon matListItemIcon>confirmation_number</mat-icon><span matListItemTitle>Tickets</span></a>
          @if (auth.isStaff()) {
            <a mat-list-item routerLink="/planning" routerLinkActive="active"><mat-icon matListItemIcon>event</mat-icon><span matListItemTitle>Planning</span></a>
            <a mat-list-item routerLink="/equipment" routerLinkActive="active"><mat-icon matListItemIcon>devices</mat-icon><span matListItemTitle>Équipements</span></a>
          }
          <a mat-list-item routerLink="/kb" routerLinkActive="active"><mat-icon matListItemIcon>menu_book</mat-icon><span matListItemTitle>Base de connaissances</span></a>
          @if (auth.isManager()) {
            <a mat-list-item routerLink="/ai" routerLinkActive="active"><mat-icon matListItemIcon>smart_toy</mat-icon><span matListItemTitle>Propositions IA</span></a>
          }
          @if (auth.isAdmin()) {
            <a mat-list-item routerLink="/admin" routerLinkActive="active"><mat-icon matListItemIcon>admin_panel_settings</mat-icon><span matListItemTitle>Administration</span></a>
          }
        </mat-nav-list>
      </mat-sidenav>
      <mat-sidenav-content>
        <mat-toolbar color="primary">
          <span>Plateforme de gestion des interventions techniques</span>
          <span class="spacer"></span>
          <button mat-button [matMenuTriggerFor]="menu">
            <mat-icon>account_circle</mat-icon> {{ auth.user()?.fullName }} <span class="small">({{ auth.role() | roleLabel }})</span>
          </button>
          <mat-menu #menu="matMenu">
            <a mat-menu-item routerLink="/account"><mat-icon>lock</mat-icon> Mon compte</a>
            <button mat-menu-item (click)="auth.logout()"><mat-icon>logout</mat-icon> Déconnexion</button>
          </mat-menu>
        </mat-toolbar>
        <router-outlet />
      </mat-sidenav-content>
    </mat-sidenav-container>
  `,
  styles: [`
    .shell { height: 100vh; }
    .sidenav { width: 240px; background: #0f2140; color: #e5e9f0; }
    .sidenav .mat-mdc-list-item, .sidenav mat-icon { color: #e5e9f0; }
    .sidenav a.active { background: rgba(255,255,255,.12); }
    .brand { display: flex; gap: 12px; align-items: center; padding: 20px 16px; font-size: 1.1rem; }
    .brand mat-icon { font-size: 36px; width: 36px; height: 36px; }
  `],
})
export class ShellComponent {
  readonly auth = inject(AuthService);
}
