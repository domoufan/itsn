package sn.senetech.intervit.ticket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sn.senetech.intervit.domain.Ticket;
import sn.senetech.intervit.domain.TicketStatus;
import sn.senetech.intervit.notification.NotificationService;
import sn.senetech.intervit.repository.TicketRepository;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;

/** Tâche planifiée : détecte les dépassements SLA et déclenche les alertes (toutes les 5 minutes). */
@Component
public class SlaMonitorJob {

    private static final Logger log = LoggerFactory.getLogger(SlaMonitorJob.class);
    private static final EnumSet<TicketStatus> OPEN = EnumSet.of(TicketStatus.NEW, TicketStatus.ASSIGNED, TicketStatus.IN_PROGRESS, TicketStatus.ON_HOLD);

    private final TicketRepository tickets;
    private final NotificationService notifications;

    public SlaMonitorJob(TicketRepository tickets, NotificationService notifications) {
        this.tickets = tickets;
        this.notifications = notifications;
    }

    @Scheduled(fixedDelayString = "PT5M", initialDelayString = "PT1M")
    @Transactional
    public int checkBreaches() {
        Instant now = Instant.now();
        int count = 0;
        List<Ticket> response = tickets.findResponseBreaches(OPEN, now);
        for (Ticket t : response) {
            t.setSlaResponseBreached(true);
            notifications.slaBreached(t, "réponse");
            count++;
        }
        List<Ticket> resolution = tickets.findResolutionBreaches(OPEN, now);
        for (Ticket t : resolution) {
            t.setSlaResolutionBreached(true);
            notifications.slaBreached(t, "résolution");
            count++;
        }
        tickets.saveAll(response);
        tickets.saveAll(resolution);
        if (count > 0) log.warn("Dépassements SLA détectés : {} (réponse={}, résolution={})", count, response.size(), resolution.size());
        return count;
    }
}
