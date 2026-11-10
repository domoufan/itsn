import { Component, Directive, ElementRef, effect, inject, input, computed } from '@angular/core';

/** Logo ITSN : hexagone bleu nuit, cœur vert dégradé, croix d'intervention. */
@Component({
  selector: 'itsn-logo',
  template: `
    <svg [attr.width]="size()" [attr.height]="size()" viewBox="0 0 64 64" aria-label="ITSN">
      <defs><linearGradient id="itsnG" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#62D84E"/><stop offset="1" stop-color="#1F9D5A"/></linearGradient></defs>
      <path d="M32 3 58 17.5v29L32 61 6 46.5v-29z" fill="#032D42"/>
      <path d="M32 11 50 21v22L32 53 14 43V21z" fill="url(#itsnG)"/>
      <path d="M22 36h7v-7h6v7h7v6h-7v7h-6v-7h-7z" fill="#032D42"/>
    </svg>`,
  styles: [':host { display: inline-flex; filter: drop-shadow(0 4px 10px rgba(98,216,78,.35)); }'],
})
export class LogoComponent { readonly size = input(40); }

/** Anime un nombre de 0 vers sa valeur (compteur de KPI). */
@Directive({ selector: '[countUp]' })
export class CountUpDirective {
  readonly countUp = input.required<number | null | undefined>();
  readonly decimals = input(0);
  readonly suffix = input('');
  private readonly el = inject(ElementRef<HTMLElement>);
  constructor() {
    effect(() => {
      const target = this.countUp();
      if (target === null || target === undefined) { this.el.nativeElement.textContent = '—'; return; }
      const reduced = matchMedia('(prefers-reduced-motion: reduce)').matches;
      const start = performance.now(), dur = reduced ? 0 : 900;
      const step = (t: number) => {
        const p = dur === 0 ? 1 : Math.min(1, (t - start) / dur);
        const eased = 1 - Math.pow(1 - p, 3);
        this.el.nativeElement.textContent = (target * eased).toFixed(this.decimals()).replace('.', ',') + this.suffix();
        if (p < 1) requestAnimationFrame(step);
      };
      requestAnimationFrame(step);
    });
  }
}

/** Anneau de progression (taux SLA, satisfaction). */
@Component({
  selector: 'itsn-ring',
  template: `
    <svg [attr.width]="size()" [attr.height]="size()" viewBox="0 0 120 120">
      <circle cx="60" cy="60" r="50" fill="none" stroke="var(--itsn-line)" stroke-width="12"/>
      <circle cx="60" cy="60" r="50" fill="none" [attr.stroke]="color()" stroke-width="12" stroke-linecap="round"
        [attr.stroke-dasharray]="circ" [attr.stroke-dashoffset]="offset()" transform="rotate(-90 60 60)" class="arc"
        [style.--dash]="circ"/>
      <text x="60" y="56" text-anchor="middle" class="val">{{ label() }}</text>
      <text x="60" y="76" text-anchor="middle" class="sub">{{ sub() }}</text>
    </svg>`,
  styles: [`
    .arc { transition: stroke-dashoffset 1.2s var(--itsn-ease); animation: dash 1.2s var(--itsn-ease) both; }
    .val { font-size: 22px; font-weight: 800; fill: var(--itsn-navy-800); }
    .sub { font-size: 10px; fill: var(--itsn-text-2); font-weight: 600; letter-spacing: .05em; }
  `],
})
export class RingComponent {
  readonly value = input<number | null | undefined>(0); // 0..100
  readonly label = input('');
  readonly sub = input('');
  readonly size = input(120);
  readonly circ = 2 * Math.PI * 50;
  readonly offset = computed(() => this.circ * (1 - Math.max(0, Math.min(100, this.value() ?? 0)) / 100));
  readonly color = computed(() => { const v = this.value() ?? 0; return v >= 90 ? 'var(--itsn-green-500)' : v >= 70 ? 'var(--itsn-p2)' : 'var(--itsn-p1)'; });
}

export interface DonutSlice { label: string; value: number; color: string; }

/** Graphique en anneau (répartition par priorité / catégorie), segments animés. */
@Component({
  selector: 'itsn-donut',
  template: `
    <div class="wrap">
      <svg [attr.width]="size()" [attr.height]="size()" viewBox="0 0 120 120">
        <circle cx="60" cy="60" r="46" fill="none" stroke="var(--itsn-line)" stroke-width="16"/>
        @for (s of segments(); track s.label) {
          <circle cx="60" cy="60" r="46" fill="none" [attr.stroke]="s.color" stroke-width="16" stroke-linecap="butt"
            [attr.stroke-dasharray]="s.len + ' ' + (circ - s.len)" [attr.stroke-dashoffset]="-s.start" transform="rotate(-90 60 60)" class="seg" [style.animation-delay.ms]="s.delay"/>
        }
        <text x="60" y="57" text-anchor="middle" class="val">{{ total() }}</text>
        <text x="60" y="74" text-anchor="middle" class="sub">{{ centerLabel() }}</text>
      </svg>
      <ul class="legend">
        @for (s of segments(); track s.label) {
          <li><span class="dot" [style.background]="s.color"></span><span class="lbl">{{ s.label }}</span><strong>{{ s.value }}</strong><span class="pct">{{ s.pct }} %</span></li>
        }
      </ul>
    </div>`,
  styles: [`
    .wrap { display: flex; gap: 20px; align-items: center; flex-wrap: wrap; }
    .seg { animation: segIn 1s var(--itsn-ease) both; transform-origin: 60px 60px; }
    @keyframes segIn { from { opacity: 0; transform: rotate(-30deg); } }
    .val { font-size: 24px; font-weight: 800; fill: var(--itsn-navy-800); }
    .sub { font-size: 9px; fill: var(--itsn-text-2); font-weight: 600; letter-spacing: .08em; text-transform: uppercase; }
    .legend { list-style: none; margin: 0; padding: 0; display: grid; gap: 8px; min-width: 180px; }
    .legend li { display: grid; grid-template-columns: 12px 1fr auto auto; gap: 10px; align-items: center; font-size: .9rem; }
    .dot { width: 12px; height: 12px; border-radius: 4px; }
    .pct { color: var(--itsn-text-3); font-size: .8rem; min-width: 38px; text-align: right; }
  `],
})
export class DonutComponent {
  readonly slices = input<DonutSlice[]>([]);
  readonly centerLabel = input('total');
  readonly size = input(150);
  readonly circ = 2 * Math.PI * 46;
  readonly total = computed(() => this.slices().reduce((a, s) => a + s.value, 0));
  readonly segments = computed(() => {
    const total = this.total() || 1; let acc = 0;
    return this.slices().filter((s) => s.value > 0).map((s, i) => {
      const len = (s.value / total) * this.circ; const seg = { ...s, len, start: acc, pct: Math.round((100 * s.value) / total), delay: 120 * i }; acc += len; return seg;
    });
  });
}

/** État vide illustré. */
@Component({
  selector: 'itsn-empty',
  template: `
    <div class="empty">
      <svg viewBox="0 0 120 120" fill="none">
        <circle cx="60" cy="60" r="52" fill="var(--itsn-green-50)"/>
        <rect x="30" y="38" width="60" height="48" rx="8" fill="#fff" stroke="var(--itsn-green-300)" stroke-width="3"/>
        <path d="M40 54h40M40 64h28M40 74h20" stroke="var(--itsn-mint)" stroke-width="3" stroke-linecap="round"/>
        <circle cx="84" cy="36" r="12" fill="var(--itsn-green-400)"/><path d="M78 36l4 4 8-8" stroke="#fff" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
      <strong>{{ title() }}</strong>
      <div>{{ text() }}</div>
      <ng-content />
    </div>`,
})
export class EmptyComponent { readonly title = input('Rien à afficher'); readonly text = input(''); }
