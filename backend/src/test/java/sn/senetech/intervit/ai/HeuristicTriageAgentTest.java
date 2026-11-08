package sn.senetech.intervit.ai;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import sn.senetech.intervit.domain.Role;
import sn.senetech.intervit.domain.Ticket;
import sn.senetech.intervit.domain.User;
import sn.senetech.intervit.repository.TicketRepository;
import sn.senetech.intervit.repository.UserRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;

/** TU-05 : solution de repli heuristique (exigence ENF-07 : fonctionnement dégradé sans IA). */
class HeuristicTriageAgentTest {

    @Test
    void classifiesSecurityIncidentAsUrgent() {
        UserRepository users = Mockito.mock(UserRepository.class);
        TicketRepository tickets = Mockito.mock(TicketRepository.class);
        User tech = Mockito.mock(User.class);
        Mockito.when(tech.getId()).thenReturn(7L);
        Mockito.when(tech.getSkills()).thenReturn("SECURITY,SERVER");
        Mockito.when(users.findByRoleAndActiveTrueOrderByFullName(Role.TECHNICIAN)).thenReturn(List.of(tech));
        Mockito.when(tickets.countByAssigneeIdAndStatusIn(anyLong(), any())).thenReturn(2L);

        Ticket t = new Ticket();
        t.setTitle("E-mail de phishing reçu par plusieurs agents");
        t.setDescription("Plusieurs collègues ont cliqué sur un lien suspect, c'est urgent.");

        TriageResult r = new HeuristicTriageAgent(users, tickets).triage(t);

        assertThat(r.isSuccess()).isTrue();
        assertThat(r.output().categoryCode()).isEqualTo("SECURITY");
        assertThat(r.output().impact()).isEqualTo(1);
        assertThat(r.output().urgency()).isEqualTo(1);
        assertThat(r.output().suggestedTechnicianId()).isEqualTo(7L);
        assertThat(r.model()).isEqualTo("heuristic");
    }

    @Test
    void fallsBackToOtherCategory() {
        UserRepository users = Mockito.mock(UserRepository.class);
        TicketRepository tickets = Mockito.mock(TicketRepository.class);
        Mockito.when(users.findByRoleAndActiveTrueOrderByFullName(Role.TECHNICIAN)).thenReturn(List.of());
        Ticket t = new Ticket();
        t.setTitle("Question diverse");
        t.setDescription("Je voudrais des informations générales.");
        TriageResult r = new HeuristicTriageAgent(users, tickets).triage(t);
        assertThat(r.output().categoryCode()).isEqualTo("OTHER");
        assertThat(r.output().suggestedTechnicianId()).isNull();
        assertThat(r.output().confidence()).isLessThan(0.5);
    }
}
