package sn.senetech.intervit.ticket;

import org.junit.jupiter.api.Test;
import sn.senetech.intervit.domain.TicketStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static sn.senetech.intervit.domain.TicketStatus.*;

/** TU-02 : machine à états du ticket (exigence EF-04). */
class TicketStatusTest {

    @Test
    void allowedTransitions() {
        assertThat(NEW.canTransitionTo(ASSIGNED)).isTrue();
        assertThat(ASSIGNED.canTransitionTo(IN_PROGRESS)).isTrue();
        assertThat(IN_PROGRESS.canTransitionTo(ON_HOLD)).isTrue();
        assertThat(ON_HOLD.canTransitionTo(IN_PROGRESS)).isTrue();
        assertThat(IN_PROGRESS.canTransitionTo(RESOLVED)).isTrue();
        assertThat(RESOLVED.canTransitionTo(CLOSED)).isTrue();
        assertThat(RESOLVED.canTransitionTo(IN_PROGRESS)).as("réouverture").isTrue();
    }

    @Test
    void forbiddenTransitions() {
        assertThat(NEW.canTransitionTo(RESOLVED)).isFalse();
        assertThat(NEW.canTransitionTo(IN_PROGRESS)).isFalse();
        assertThat(CLOSED.canTransitionTo(IN_PROGRESS)).isFalse();
        assertThat(CANCELLED.canTransitionTo(ASSIGNED)).isFalse();
        assertThat(ON_HOLD.canTransitionTo(RESOLVED)).isFalse();
    }

    @Test
    void terminalStates() {
        assertThat(CLOSED.isTerminal()).isTrue();
        assertThat(CANCELLED.isTerminal()).isTrue();
        assertThat(RESOLVED.isTerminal()).isFalse();
        assertThat(RESOLVED.isOpen()).isFalse();
        assertThat(IN_PROGRESS.isOpen()).isTrue();
    }

    @Test
    void referenceFormat() {
        assertThat(TicketReferenceService.format(sn.senetech.intervit.domain.TicketType.INCIDENT, 2026, 42)).isEqualTo("INC-2026-00042");
        assertThat(TicketReferenceService.format(sn.senetech.intervit.domain.TicketType.REQUEST, 2026, 7)).isEqualTo("REQ-2026-00007");
    }
}
