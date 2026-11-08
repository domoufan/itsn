import { describe, expect, it } from 'vitest';
import { TicketFormComponent } from './ticket-form.component';

// TU-F02 : la matrice affichée côté client est identique à celle du serveur (PriorityMatrixTest.java)
describe('Matrice de priorité (affichage)', () => {
  const compute = TicketFormComponent.prototype.compute;
  it('reproduit la matrice impact x urgence', () => {
    expect(compute(1, 1)).toBe('P1');
    expect(compute(1, 2)).toBe('P2');
    expect(compute(2, 1)).toBe('P2');
    expect(compute(2, 2)).toBe('P3');
    expect(compute(1, 3)).toBe('P3');
    expect(compute(3, 3)).toBe('P4');
    expect(compute(2, 3)).toBe('P4');
  });
});
