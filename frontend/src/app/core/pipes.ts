import { Pipe, PipeTransform } from '@angular/core';
import { PRIORITY_LABELS, Priority, ROLE_LABELS, Role, STATUS_LABELS, TicketStatus } from './models';

@Pipe({ name: 'statusLabel' })
export class StatusLabelPipe implements PipeTransform {
  transform(v: string | undefined | null): string { return v ? (STATUS_LABELS[v as TicketStatus] ?? v) : ''; }
}

@Pipe({ name: 'priorityLabel' })
export class PriorityLabelPipe implements PipeTransform {
  transform(v: string | undefined | null): string { return v ? (PRIORITY_LABELS[v as Priority] ?? v) : ''; }
}

@Pipe({ name: 'roleLabel' })
export class RoleLabelPipe implements PipeTransform {
  transform(v: string | undefined | null): string { return v ? (ROLE_LABELS[v as Role] ?? v) : ''; }
}

/** Temps restant avant une échéance, lisible (ex. "dans 3 h", "dépassé de 2 j"). */
@Pipe({ name: 'dueIn' })
export class DueInPipe implements PipeTransform {
  transform(iso: string | undefined | null): string {
    if (!iso) return '';
    const diff = new Date(iso).getTime() - Date.now();
    const abs = Math.abs(diff);
    const h = Math.floor(abs / 3_600_000), d = Math.floor(h / 24), m = Math.floor((abs % 3_600_000) / 60_000);
    const txt = d > 0 ? `${d} j ${h % 24} h` : h > 0 ? `${h} h ${m} min` : `${m} min`;
    return diff >= 0 ? `dans ${txt}` : `dépassé de ${txt}`;
  }
}
