package sn.senetech.itsn.ai;

import sn.senetech.itsn.domain.Ticket;

/** Contrat de l'agent de triage : une implémentation réelle (API Claude) et une simulation locale. */
public interface TriageAgent {
    TriageResult triage(Ticket ticket);
    String providerName();
}
