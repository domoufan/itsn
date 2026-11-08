import { describe, expect, it } from 'vitest';
import { DueInPipe, PriorityLabelPipe, StatusLabelPipe } from './pipes';

// TU-F01 : libellés et échéances affichés à l'utilisateur
describe('Pipes d\'affichage', () => {
  it('traduit les états et priorités', () => {
    expect(new StatusLabelPipe().transform('IN_PROGRESS')).toBe('En cours');
    expect(new StatusLabelPipe().transform(null)).toBe('');
    expect(new PriorityLabelPipe().transform('P1')).toBe('P1 – Critique');
  });

  it('calcule le temps restant ou le dépassement', () => {
    const pipe = new DueInPipe();
    const inTwoHours = new Date(Date.now() + 2 * 3_600_000 + 5 * 60_000).toISOString();
    const yesterday = new Date(Date.now() - 26 * 3_600_000).toISOString();
    expect(pipe.transform(inTwoHours)).toMatch(/^dans 2 h/);
    expect(pipe.transform(yesterday)).toMatch(/^dépassé de 1 j 2 h/);
    expect(pipe.transform(undefined)).toBe('');
  });
});
