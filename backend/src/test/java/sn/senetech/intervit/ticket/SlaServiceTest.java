package sn.senetech.intervit.ticket;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import sn.senetech.intervit.domain.Priority;
import sn.senetech.intervit.domain.SlaPolicy;
import sn.senetech.intervit.domain.Ticket;
import sn.senetech.intervit.repository.SlaPolicyRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/** TU-03 : calcul des échéances SLA et détection des dépassements (exigences EF-05, EF-06). */
class SlaServiceTest {

    @Test
    void appliesDeadlinesFromPolicy() {
        SlaPolicyRepository repo = Mockito.mock(SlaPolicyRepository.class);
        SlaPolicy p1 = new SlaPolicy();
        p1.setPriority(Priority.P1); p1.setLabel("Critique"); p1.setResponseMinutes(30); p1.setResolutionMinutes(240);
        Mockito.when(repo.findByPriority(Priority.P1)).thenReturn(Optional.of(p1));

        Ticket t = new Ticket();
        Instant created = Instant.parse("2026-11-10T08:00:00Z");
        t.setCreatedAt(created);
        t.setPriority(Priority.P1);
        new SlaService(repo).applyDeadlines(t);

        assertThat(t.getResponseDueAt()).isEqualTo(created.plus(Duration.ofMinutes(30)));
        assertThat(t.getResolutionDueAt()).isEqualTo(created.plus(Duration.ofHours(4)));
    }

    @Test
    void detectsBreaches() {
        Ticket t = new Ticket();
        Instant created = Instant.parse("2026-11-10T08:00:00Z");
        t.setCreatedAt(created);
        t.setResponseDueAt(created.plus(Duration.ofMinutes(30)));
        t.setResolutionDueAt(created.plus(Duration.ofHours(4)));

        assertThat(SlaService.isResponseBreached(t, created.plus(Duration.ofMinutes(20)))).isFalse();
        assertThat(SlaService.isResponseBreached(t, created.plus(Duration.ofMinutes(31)))).isTrue();
        t.setResolvedAt(created.plus(Duration.ofHours(5)));
        assertThat(SlaService.isResolutionBreached(t, Instant.now())).isTrue();
    }
}
