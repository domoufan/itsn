import { Component, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AuthService } from '../core/auth.service';
import { RoleLabelPipe } from '../core/pipes';
import { LogoComponent } from '../shared/ui';

/** Gabarit principal : navigation latérale sombre (bleu nuit) avec indicateur vert, en-tête léger. */
@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatSidenavModule, MatIconModule, MatButtonModule, MatMenuModule, MatTooltipModule, RoleLabelPipe, LogoComponent],
  template: `
    <mat-sidenav-container class="shell">
      <mat-sidenav mode="side" opened class="sidenav" [class.collapsed]="collapsed()">
        <div class="brand">
          <itsn-logo [size]="38" />
          @if (!collapsed()) { <div><div class="brand-name">ITSN</div><div class="brand-sub">SénéTech Services</div></div> }
        </div>
        <nav>
          @for (item of items(); track item.path) {
            <a [routerLink]="item.path" routerLinkActive="active" class="nav-item" [matTooltip]="collapsed() ? item.label : ''" matTooltipPosition="right">
              <span class="ico"><mat-icon>{{ item.icon }}</mat-icon></span>
              @if (!collapsed()) { <span class="lbl">{{ item.label }}</span> }
            </a>
          }
        </nav>
        <div class="sidenav-foot">
          <button mat-icon-button (click)="collapsed.set(!collapsed())" [matTooltip]="collapsed() ? 'Déplier' : 'Replier'" matTooltipPosition="right">
            <mat-icon>{{ collapsed() ? 'chevron_right' : 'chevron_left' }}</mat-icon>
          </button>
          @if (!collapsed()) { <span class="small ver">v1.0 · ITSM</span> }
        </div>
      </mat-sidenav>
      <mat-sidenav-content>
        <header class="topbar">
          <div class="crumb"><mat-icon>bolt</mat-icon> Plateforme de gestion des interventions techniques</div>
          <span class="spacer"></span>
          <button mat-button [matMenuTriggerFor]="menu" class="user-btn">
            <span class="avatar">{{ initials() }}</span>
            <span class="user-txt"><strong>{{ auth.user()?.fullName }}</strong><span class="small muted">{{ auth.role() | roleLabel }}</span></span>
            <mat-icon>expand_more</mat-icon>
          </button>
          <mat-menu #menu="matMenu">
            <a mat-menu-item routerLink="/account"><mat-icon>manage_accounts</mat-icon> Mon compte</a>
            <button mat-menu-item (click)="auth.logout()"><mat-icon>logout</mat-icon> Déconnexion</button>
          </mat-menu>
        </header>
        <router-outlet />
      </mat-sidenav-content>
    </mat-sidenav-container>
  `,
  styles: [`
    .shell { height: 100vh; }
    .sidenav { width: 248px; background: linear-gradient(180deg, var(--itsn-navy-800) 0%, var(--itsn-navy-900) 100%); color: #DCEEE4; border-right: none !important; transition: width var(--itsn-med) var(--itsn-ease); display: flex; flex-direction: column; }
    .sidenav.collapsed { width: 76px; }
    .brand { display: flex; gap: 12px; align-items: center; padding: 22px 18px 18px; }
    .brand-name { font-weight: 800; font-size: 1.25rem; letter-spacing: .08em; color: #fff; }
    .brand-sub { font-size: .72rem; color: var(--itsn-mint); font-weight: 600; letter-spacing: .04em; }
    nav { display: flex; flex-direction: column; gap: 4px; padding: 6px 12px; flex: 1; }
    .nav-item { display: flex; align-items: center; gap: 12px; padding: 10px 12px; border-radius: 12px; color: #B8CFC4; text-decoration: none; font-weight: 500; position: relative; transition: background var(--itsn-fast), color var(--itsn-fast), transform var(--itsn-fast); }
    .nav-item:hover { background: rgba(255,255,255,.06); color: #fff; transform: translateX(2px); text-decoration: none; }
    .nav-item.active { background: rgba(98,216,78,.14); color: #fff; }
    .nav-item.active::before { content: ''; position: absolute; left: -12px; top: 8px; bottom: 8px; width: 4px; border-radius: 4px; background: var(--itsn-green-300); box-shadow: 0 0 12px var(--itsn-green-300); }
    .ico { width: 36px; height: 36px; border-radius: 10px; display: grid; place-items: center; background: rgba(255,255,255,.05); transition: background var(--itsn-fast); }
    .nav-item.active .ico { background: linear-gradient(135deg, var(--itsn-green-500), var(--itsn-green-300)); color: var(--itsn-navy-900); }
    .sidenav-foot { display: flex; align-items: center; justify-content: space-between; padding: 10px 14px 16px; color: #8FB3A3; }
    .sidenav-foot button { color: #B8CFC4; }
    .ver { opacity: .7; }
    .topbar { display: flex; align-items: center; gap: 12px; padding: 10px 32px; background: rgba(255,255,255,.75); backdrop-filter: blur(8px); border-bottom: 1px solid var(--itsn-line); position: sticky; top: 0; z-index: 5; }
    .crumb { display: flex; align-items: center; gap: 8px; color: var(--itsn-text-2); font-weight: 600; font-size: .9rem; }
    .crumb mat-icon { color: var(--itsn-green-500); }
    .user-btn { height: auto !important; padding: 4px 10px !important; }
    .avatar { width: 34px; height: 34px; border-radius: 50%; display: grid; place-items: center; background: linear-gradient(135deg, var(--itsn-green-500), var(--itsn-green-300)); color: #fff; font-weight: 800; font-size: .85rem; margin-right: 10px; }
    .user-txt { display: inline-flex; flex-direction: column; align-items: flex-start; line-height: 1.15; text-align: left; margin-right: 6px; }
  `],
})
export class ShellComponent {
  readonly auth = inject(AuthService);
  readonly collapsed = signal(false);
  readonly items = () => {
    const staff = this.auth.isStaff(), manager = this.auth.isManager(), admin = this.auth.isAdmin();
    return [
      ...(staff ? [{ path: '/dashboard', icon: 'space_dashboard', label: 'Tableau de bord' }] : []),
      { path: '/tickets', icon: 'confirmation_number', label: 'Tickets' },
      ...(staff ? [{ path: '/planning', icon: 'event_available', label: 'Planning' }, { path: '/equipment', icon: 'devices', label: 'Équipements' }] : []),
      { path: '/kb', icon: 'menu_book', label: 'Base de connaissances' },
      ...(manager ? [{ path: '/ai', icon: 'smart_toy', label: 'Propositions IA' }] : []),
      ...(admin ? [{ path: '/admin', icon: 'admin_panel_settings', label: 'Administration' }] : []),
    ];
  };
  initials() { const n = this.auth.user()?.fullName ?? '?'; return n.split(' ').filter(Boolean).slice(0, 2).map((p) => p[0]).join('').toUpperCase(); }
}
