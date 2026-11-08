package sn.senetech.intervit.domain;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** Cycle de vie d'un ticket et transitions autorisées (machine à états). */
public enum TicketStatus {
    NEW, ASSIGNED, IN_PROGRESS, ON_HOLD, RESOLVED, CLOSED, CANCELLED;

    private static final Map<TicketStatus, Set<TicketStatus>> TRANSITIONS = Map.of(
        NEW,         EnumSet.of(ASSIGNED, CANCELLED),
        ASSIGNED,    EnumSet.of(IN_PROGRESS, ASSIGNED, CANCELLED),
        IN_PROGRESS, EnumSet.of(ON_HOLD, RESOLVED, ASSIGNED, CANCELLED),
        ON_HOLD,     EnumSet.of(IN_PROGRESS, CANCELLED),
        RESOLVED,    EnumSet.of(CLOSED, IN_PROGRESS),
        CLOSED,      EnumSet.noneOf(TicketStatus.class),
        CANCELLED,   EnumSet.noneOf(TicketStatus.class)
    );

    public boolean canTransitionTo(TicketStatus target) {
        return TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }

    public boolean isTerminal() {
        return this == CLOSED || this == CANCELLED;
    }

    public boolean isOpen() {
        return !isTerminal() && this != RESOLVED;
    }
}
