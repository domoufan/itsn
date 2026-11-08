package sn.senetech.intervit.ticket;

import sn.senetech.intervit.domain.Priority;

/**
 * Matrice de priorisation ITIL : la priorité résulte du croisement impact x urgence.
 * Échelle : 1 = élevé(e), 2 = moyen(ne), 3 = faible.
 *
 * <pre>
 *                 Urgence 1   Urgence 2   Urgence 3
 *   Impact 1        P1          P2          P3
 *   Impact 2        P2          P3          P4
 *   Impact 3        P3          P4          P4
 * </pre>
 */
public final class PriorityMatrix {

    private PriorityMatrix() {}

    public static Priority compute(int impact, int urgency) {
        if (impact < 1 || impact > 3 || urgency < 1 || urgency > 3) {
            throw new IllegalArgumentException("Impact et urgence doivent être compris entre 1 et 3");
        }
        int score = impact + urgency; // 2..6
        return switch (score) {
            case 2 -> Priority.P1;
            case 3 -> Priority.P2;
            case 4 -> Priority.P3;
            default -> Priority.P4;
        };
    }
}
