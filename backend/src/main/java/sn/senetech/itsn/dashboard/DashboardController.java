package sn.senetech.itsn.dashboard;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sn.senetech.itsn.domain.SuggestionStatus;
import sn.senetech.itsn.domain.Ticket;
import sn.senetech.itsn.domain.TicketStatus;
import sn.senetech.itsn.repository.AiSuggestionRepository;
import sn.senetech.itsn.repository.TicketRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Indicateurs de pilotage du support : volumes, respect des SLA, MTTR, charge des techniciens. */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    public record Kpis(long openTickets, long createdLast30Days, long resolvedLast30Days, long overdueOpen,
                       double slaResolutionComplianceRate, double slaResponseComplianceRate, Double meanTimeToResolveHours,
                       Double averageSatisfaction, Map<String, Long> openByPriority, Map<String, Long> byCategory,
                       List<Workload> workload, long pendingAiSuggestions, Instant computedAt) {}

    public record Workload(Long technicianId, String technicianName, long activeTickets) {}

    private static final EnumSet<TicketStatus> OPEN = EnumSet.of(TicketStatus.NEW, TicketStatus.ASSIGNED, TicketStatus.IN_PROGRESS, TicketStatus.ON_HOLD);
    private static final EnumSet<TicketStatus> ACTIVE = EnumSet.of(TicketStatus.ASSIGNED, TicketStatus.IN_PROGRESS, TicketStatus.ON_HOLD);

    private final TicketRepository tickets;
    private final AiSuggestionRepository suggestions;

    public DashboardController(TicketRepository tickets, AiSuggestionRepository suggestions) {
        this.tickets = tickets; this.suggestions = suggestions;
    }

    @GetMapping("/kpis")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','TECHNICIAN')")
    @Transactional(readOnly = true)
    public Kpis kpis(@RequestParam(defaultValue = "30") int days) {
        Instant now = Instant.now();
        Instant from = now.minus(Duration.ofDays(Math.max(1, Math.min(days, 365))));

        List<Ticket> resolved = tickets.findResolvedBetween(from, now);
        long respOk = resolved.stream().filter(t -> !t.isSlaResponseBreached()).count();
        long resOk = resolved.stream().filter(t -> !t.isSlaResolutionBreached()).count();
        Double mttr = resolved.isEmpty() ? null : resolved.stream()
            .mapToDouble(t -> Duration.between(t.getCreatedAt(), t.getResolvedAt()).toMinutes() / 60.0).average().orElse(0);
        Double satisfaction = resolved.stream().filter(t -> t.getSatisfactionScore() != null)
            .mapToInt(Ticket::getSatisfactionScore).average().isPresent()
            ? resolved.stream().filter(t -> t.getSatisfactionScore() != null).mapToInt(Ticket::getSatisfactionScore).average().getAsDouble() : null;

        Map<String, Long> byPriority = new LinkedHashMap<>();
        for (Object[] row : tickets.countOpenByPriority(OPEN)) byPriority.put(String.valueOf(row[0]), (Long) row[1]);
        Map<String, Long> byCategory = new LinkedHashMap<>();
        for (Object[] row : tickets.countByCategorySince(from)) byCategory.put(String.valueOf(row[0]), (Long) row[1]);
        List<Workload> workload = tickets.workloadByTechnician(ACTIVE).stream()
            .map(r -> new Workload((Long) r[0], (String) r[1], (Long) r[2])).toList();

        return new Kpis(
            tickets.countByStatusIn(OPEN),
            tickets.countByCreatedAtBetween(from, now),
            resolved.size(),
            tickets.countByStatusInAndResolutionDueAtBefore(OPEN, now),
            resolved.isEmpty() ? 100.0 : round(100.0 * resOk / resolved.size()),
            resolved.isEmpty() ? 100.0 : round(100.0 * respOk / resolved.size()),
            mttr == null ? null : round(mttr),
            satisfaction == null ? null : round(satisfaction),
            byPriority, byCategory, workload,
            suggestions.countByStatus(SuggestionStatus.PENDING),
            now);
    }

    private static double round(double v) { return Math.round(v * 10.0) / 10.0; }
}
