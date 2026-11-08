import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { NotifyService } from '../../core/notify.service';
import { Article, Category } from '../../core/models';

/** Base de connaissances : recherche, lecture, rédaction par le personnel (exigence EF-14). */
@Component({
  selector: 'app-kb',
  imports: [DatePipe, FormsModule, MatCardModule, MatButtonModule, MatIconModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatCheckboxModule],
  template: `
    <div class="page">
      <div class="page-header"><h1>Base de connaissances</h1>
        <div class="toolbar-row">
          <mat-form-field appearance="outline" subscriptSizing="dynamic" style="min-width:300px"><mat-label>Rechercher</mat-label><input matInput [(ngModel)]="q" (keyup.enter)="load()" /><mat-icon matSuffix>search</mat-icon></mat-form-field>
          @if (auth.isStaff()) { <button mat-flat-button color="primary" (click)="draft.set({ published: false })"><mat-icon>add</mat-icon> Rédiger</button> }
        </div>
      </div>
      @if (draft(); as d) {
        <mat-card style="margin-bottom:16px"><mat-card-content>
          <mat-form-field class="full" appearance="outline"><mat-label>Titre</mat-label><input matInput [(ngModel)]="d.title" /></mat-form-field>
          <div class="form-grid">
            <mat-form-field appearance="outline"><mat-label>Catégorie</mat-label><mat-select [(ngModel)]="d.categoryId">@for (c of categories(); track c.id) { <mat-option [value]="c.id">{{ c.name }}</mat-option> }</mat-select></mat-form-field>
            <mat-form-field appearance="outline"><mat-label>Mots clés (séparés par des virgules)</mat-label><input matInput [(ngModel)]="d.tags" /></mat-form-field>
          </div>
          <mat-form-field class="full" appearance="outline"><mat-label>Contenu (procédure, étapes numérotées)</mat-label><textarea matInput rows="10" [(ngModel)]="d.content"></textarea></mat-form-field>
          <mat-checkbox [(ngModel)]="d.published">Publié (visible des demandeurs)</mat-checkbox>
          <div><button mat-flat-button color="primary" (click)="save(d)">Enregistrer</button> <button mat-button (click)="draft.set(null)">Annuler</button></div>
        </mat-card-content></mat-card>
      }
      <div class="grid grid-2">
        @for (a of items(); track a.id) {
          <mat-card>
            <mat-card-header><mat-card-title>{{ a.title }}</mat-card-title><mat-card-subtitle>{{ a.categoryName }} · {{ a.tags }} · {{ a.views }} vues · {{ a.updatedAt | date:'dd/MM/yyyy' }} @if (!a.published) { · <em>brouillon</em> }</mat-card-subtitle></mat-card-header>
            <mat-card-content><p class="pre" [class.small]="!expanded().has(a.id)">{{ expanded().has(a.id) ? a.content : (a.content.length > 220 ? a.content.slice(0, 220) + '…' : a.content) }}</p></mat-card-content>
            <mat-card-actions>
              <button mat-button (click)="toggle(a)">{{ expanded().has(a.id) ? 'Réduire' : 'Lire' }}</button>
              @if (auth.isStaff()) { <button mat-button (click)="draft.set({ ...a })">Modifier</button> }
              @if (auth.isManager()) { <button mat-button color="warn" (click)="remove(a)">Supprimer</button> }
            </mat-card-actions>
          </mat-card>
        } @empty { <p class="muted">Aucun article.</p> }
      </div>
    </div>
  `,
})
export class KbComponent {
  private readonly api = inject(ApiService);
  private readonly notify = inject(NotifyService);
  readonly auth = inject(AuthService);
  readonly items = signal<Article[]>([]);
  readonly categories = signal<Category[]>([]);
  readonly draft = signal<Partial<Article> | null>(null);
  readonly expanded = signal(new Set<number>());
  q = '';
  constructor() { this.api.categories().subscribe((c) => this.categories.set(c)); this.load(); }
  load() { this.api.articles(this.q).subscribe((p) => this.items.set(p.content)); }
  toggle(a: Article) {
    const s = new Set(this.expanded());
    if (s.has(a.id)) s.delete(a.id); else { s.add(a.id); this.api.article(a.id).subscribe(); }
    this.expanded.set(s);
  }
  save(d: Partial<Article>) { this.api.saveArticle(d).subscribe({ next: () => { this.notify.success('Article enregistré'); this.draft.set(null); this.load(); }, error: (e) => this.notify.error(e) }); }
  remove(a: Article) { if (confirm(`Supprimer « ${a.title} » ?`)) this.api.deleteArticle(a.id).subscribe({ next: () => this.load(), error: (e) => this.notify.error(e) }); }
}
