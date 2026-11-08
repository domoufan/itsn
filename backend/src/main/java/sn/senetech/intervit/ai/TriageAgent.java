package sn.senetech.intervit.ai;

import sn.senetech.intervit.domain.Ticket;

/** Contrat de l'agent de triage : une implémentation réelle (API Claude) et une simulation locale. */
public interface TriageAgent {
    TriageResult triage(Ticket ticket);
    String providerName();
}
