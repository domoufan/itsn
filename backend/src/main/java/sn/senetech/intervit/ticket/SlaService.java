package sn.senetech.intervit.ticket;

import org.springframework.stereotype.Service;
import sn.senetech.intervit.common.NotFoundException;
import sn.senetech.intervit.domain.Priority;
import sn.senetech.intervit.domain.SlaPolicy;
import sn.senetech.intervit.domain.Ticket;
import sn.senetech.intervit.repository.SlaPolicyRepository;

import java.time.Duration;
import java.time.Instant;

/** Calcule les échéances SLA d'un ticket à partir de la politique associée à sa priorité. */
@Service
public class SlaService {

    private final SlaPolicyRepository repository;

    public SlaService(SlaPolicyRepository repository) {
        this.repository = repository;
    }

    public SlaPolicy policyFor(Priority priority) {
        return repository.findByPriority(priority).orElseThrow(() -> new NotFoundException("Politique SLA", priority));
    }

    /** (Re)calcule les échéances à partir de la date de création du ticket. */
    public void applyDeadlines(Ticket ticket) {
        SlaPolicy policy = policyFor(ticket.getPriority());
        Instant base = ticket.getCreatedAt() == null ? Instant.now() : ticket.getCreatedAt();
        ticket.setResponseDueAt(base.plus(Duration.ofMinutes(policy.getResponseMinutes())));
        ticket.setResolutionDueAt(base.plus(Duration.ofMinutes(policy.getResolutionMinutes())));
    }

    public static boolean isResponseBreached(Ticket t, Instant now) {
        Instant ref = t.getFirstResponseAt() != null ? t.getFirstResponseAt() : now;
        return ref.isAfter(t.getResponseDueAt());
    }

    public static boolean isResolutionBreached(Ticket t, Instant now) {
        Instant ref = t.getResolvedAt() != null ? t.getResolvedAt() : now;
        return ref.isAfter(t.getResolutionDueAt());
    }
}
