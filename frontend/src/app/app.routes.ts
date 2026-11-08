import { Routes } from '@angular/router';
import { authGuard, roleGuard } from './core/guards';

export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./features/auth/login.component').then((m) => m.LoginComponent) },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./layout/shell.component').then((m) => m.ShellComponent),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'tickets' },
      { path: 'dashboard', canActivate: [roleGuard(['ADMIN', 'MANAGER', 'TECHNICIAN'])], loadComponent: () => import('./features/dashboard/dashboard.component').then((m) => m.DashboardComponent) },
      { path: 'tickets', loadComponent: () => import('./features/tickets/ticket-list.component').then((m) => m.TicketListComponent) },
      { path: 'tickets/new', loadComponent: () => import('./features/tickets/ticket-form.component').then((m) => m.TicketFormComponent) },
      { path: 'tickets/:id', loadComponent: () => import('./features/tickets/ticket-detail.component').then((m) => m.TicketDetailComponent) },
      { path: 'planning', canActivate: [roleGuard(['ADMIN', 'MANAGER', 'TECHNICIAN'])], loadComponent: () => import('./features/interventions/planning.component').then((m) => m.PlanningComponent) },
      { path: 'equipment', canActivate: [roleGuard(['ADMIN', 'MANAGER', 'TECHNICIAN'])], loadComponent: () => import('./features/equipment/equipment.component').then((m) => m.EquipmentComponent) },
      { path: 'kb', loadComponent: () => import('./features/kb/kb.component').then((m) => m.KbComponent) },
      { path: 'ai', canActivate: [roleGuard(['ADMIN', 'MANAGER'])], loadComponent: () => import('./features/ai/ai-review.component').then((m) => m.AiReviewComponent) },
      { path: 'admin', canActivate: [roleGuard(['ADMIN'])], loadComponent: () => import('./features/admin/admin.component').then((m) => m.AdminComponent) },
      { path: 'account', loadComponent: () => import('./features/auth/account.component').then((m) => m.AccountComponent) },
    ],
  },
  { path: '**', redirectTo: '' },
];
