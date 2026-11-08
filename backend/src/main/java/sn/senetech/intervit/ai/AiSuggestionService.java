package sn.senetech.intervit.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.senetech.intervit.ai.AiDtos.ReviewRequest;
import sn.senetech.intervit.ai.AiDtos.SuggestionResponse;
import sn.senetech.intervit.auth.CurrentUserService;
import sn.senetech.intervit.common.AuditService;
import sn.senetech.intervit.common.BusinessException;
import sn.senetech.intervit.common.NotFoundException;
import sn.senetech.intervit.config.AppProperties;
import sn.senetech.intervit.domain.*;
import sn.senetech.intervit.notification.NotificationService;
import sn.senetech.intervit.repository.*;
import sn.senetech.intervit.ticket.PriorityMatrix;
import sn.senetech.intervit.ticket.SlaService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Orchestration du pipeline agentique côté métier :
 * garde-fous (activation, budget journalier) → exécution de l'agent (ou repli heuristique) →
 * enregistrement de la proposition → revue humaine → application contrôlée au ticket.
 */
@Service
public class AiSuggestionService {

    private static final Logger log = LoggerFactory.getLogger(AiSuggestionService.class);

    private final AiSuggestionRepository suggestions;
    private final TicketRepository tickets;
    private final CategoryRepository categories;
    private final UserRepository users;
    private final TicketHistoryRepository history;
    private final ClaudeTriageAgent claudeAgent;
    private final HeuristicTriageAgent heuristicAgent;
    private final SlaService slaService;
    private final CurrentUserService currentUser;
    private final NotificationService notifications;
    private final AuditService audit;
    private final AppProperties.Ai ai;

    public AiSuggestionService(AiSuggestionRepository suggestions, TicketRepository tickets, CategoryRepository categories,
                               UserRepository users, TicketHistoryRepository history, ClaudeTriageAgent claudeAgent,
                               HeuristicTriageAgent heuristicAgent, SlaService slaService, CurrentUserService currentUser,
                               NotificationService notifications, AuditService audit, AppProperties properties) {
        this.suggestions = suggestions; this.tickets = tickets; this.categories = categories; this.users = users;
        this.history = history; this.claudeAgent = claudeAgent; this.heuristicAgent = heuristicAgent; this.slaService = slaService;
        this.currentUser = currentUser; this.notifications = notifications; this.audit = audit; this.ai = properties.ai();
    }

    public boolean aiAvailable() {
        return ai.enabled() && ai.apiKey() != null && !ai.apiKey().isBlank();
    }

    public BigDecimal spentToday() {
        Instant startOfDay = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
        return suggestions.sumCostSince(startOfDay);
    }

    public boolean budgetExceeded() {
        return spentToday().compareTo(ai.dailyBudgetUsd()) >= 0;
    }

    /**
     * Lance le triage d'un ticket. {@code forceFallback} impose la simulation locale (démonstration sans clé API).
     * La proposition est enregistrée avec le statut PENDING (ou FAILED) et n'est jamais appliquée ici.
     */
    @Transactional
    public SuggestionResponse triage(Long ticketId, boolean forceFallback) {
        Ticket ticket = tickets.findById(ticketId).orElseThrow(() -> new NotFoundException("Ticket", ticketId));
        if (ticket.getStatus().isTerminal() || ticket.getStatus() == TicketStatus.RESOLVED) {
            throw new BusinessException("Le triage ne concerne que les tickets ouverts");
        }
        boolean hasPending = suggestions.findByTicketIdOrderByCreatedAtDesc(ticketId).stream().anyMatch(s -> s.getStatus() == SuggestionStatus.PENDING);
        if (hasPending) throw new BusinessException("Une proposition est déjà en attente de validation pour ce ticket");

        TriageAgent agent;
        if (forceFallback || !aiAvailable()) {
            agent = heuristicAgent;
        } else if (budgetExceeded()) {
            log.warn("Budget IA journalier atteint ({} USD) : repli heuristique", ai.dailyBudgetUsd());
            agent = heuristicAgent;
        } else {
            agent = claudeAgent;
        }

        TriageResult result = agent.triage(ticket);
        AiSuggestion s = new AiSuggestion();
        s.setTicket(ticket);
        s.setModel(result.model());
        s.setToolCalls(result.toolCalls());
        s.setInputTokens(result.inputTokens());
        s.setOutputTokens(result.outputTokens());
        s.setLatencyMs((int) Math.min(Integer.MAX_VALUE, result.latencyMs()));
        s.setEstimatedCostUsd(estimateCost(result.inputTokens(), result.outputTokens()));
        s.setRawResponse(result.rawResponse() == null ? null : truncate(result.rawResponse(), 20000));

        if (result.isSuccess()) {
            TriageOutput out = result.output();
            s.setStatus(SuggestionStatus.PENDING);
            s.setSuggestedCategory(categories.findByCode(out.categoryCode()).orElse(null));
            s.setSuggestedImpact(out.impact().shortValue());
            s.setSuggestedUrgency(out.urgency().shortValue());
            s.setSuggestedPriority(PriorityMatrix.compute(out.impact(), out.urgency()));
            if (out.suggestedTechnicianId() != null) {
                // Validation côté serveur : le technicien proposé doit exister, être actif et avoir le bon rôle
                users.findById(out.suggestedTechnicianId())
                    .filter(u -> u.getRole() == Role.TECHNICIAN && u.isActive())
                    .ifPresent(s::setSuggestedAssignee);
            }
            s.setSummary(out.summary());
            s.setRationale(out.rationale());
            if (out.relatedArticleIds() != null && !out.relatedArticleIds().isEmpty()) {
                s.setRelatedArticleIds(out.relatedArticleIds().stream().filter(Objects::nonNull).map(String::valueOf).collect(Collectors.joining(",")));
            }
        } else {
            s.setStatus(SuggestionStatus.FAILED);
            s.setErrorMessage(result.error());
        }
        s = suggestions.save(s);
        audit.record(currentUser.currentUserId(), "AI_TRIAGE_RUN", "TICKET", ticketId,
            "provider=%s status=%s tools=%d cost=%s".formatted(result.model(), s.getStatus(), result.toolCalls(), s.getEstimatedCostUsd()));
        return SuggestionResponse.from(s);
    }

    /** Décision humaine : seule voie d'application d'une proposition au ticket. */
    @Transactional
    public SuggestionResponse review(Long suggestionId, ReviewRequest req) {
        AiSuggestion s = suggestions.findById(suggestionId).orElseThrow(() -> new NotFoundException("Proposition", suggestionId));
        if (s.getStatus() != SuggestionStatus.PENDING) throw new BusinessException("Cette proposition a déjà été traitée");
        User reviewer = currentUser.currentUser();
        Ticket t = s.getTicket();
        if (t.getStatus().isTerminal() || t.getStatus() == TicketStatus.RESOLVED) throw new BusinessException("Le ticket n'est plus ouvert");

        boolean applyCategory = false, applyPriority = false, applyAssignee = false;
        switch (req.decision()) {
            case ACCEPT -> { applyCategory = true; applyPriority = true; applyAssignee = true; }
            case PARTIAL -> { applyCategory = req.applyCategory(); applyPriority = req.applyPriority(); applyAssignee = req.applyAssignee(); }
            case REJECT -> { /* rien n'est appliqué */ }
        }

        if (applyCategory && s.getSuggestedCategory() != null && !Objects.equals(s.getSuggestedCategory().getId(), t.getCategory() == null ? null : t.getCategory().getId())) {
            history.save(TicketHistory.of(t, reviewer, "category", t.getCategory() == null ? null : t.getCategory().getCode(), s.getSuggestedCategory().getCode()));
            t.setCategory(s.getSuggestedCategory());
        }
        if (applyPriority && s.getSuggestedImpact() != null && s.getSuggestedUrgency() != null) {
            if (s.getSuggestedImpact() != t.getImpact()) { history.save(TicketHistory.of(t, reviewer, "impact", t.getImpact(), s.getSuggestedImpact())); t.setImpact(s.getSuggestedImpact()); }
            if (s.getSuggestedUrgency() != t.getUrgency()) { history.save(TicketHistory.of(t, reviewer, "urgency", t.getUrgency(), s.getSuggestedUrgency())); t.setUrgency(s.getSuggestedUrgency()); }
            Priority p = PriorityMatrix.compute(t.getImpact(), t.getUrgency());
            if (p != t.getPriority()) {
                history.save(TicketHistory.of(t, reviewer, "priority", t.getPriority(), p));
                t.setPriority(p);
                slaService.applyDeadlines(t);
            }
        }
        if (applyAssignee && s.getSuggestedAssignee() != null && s.getSuggestedAssignee().isActive()) {
            if (t.getAssignee() == null || !t.getAssignee().getId().equals(s.getSuggestedAssignee().getId())) {
                history.save(TicketHistory.of(t, reviewer, "assignee", t.getAssignee() == null ? null : t.getAssignee().getFullName(), s.getSuggestedAssignee().getFullName()));
                t.setAssignee(s.getSuggestedAssignee());
                if (t.getStatus() == TicketStatus.NEW) {
                    history.save(TicketHistory.of(t, reviewer, "status", TicketStatus.NEW, TicketStatus.ASSIGNED));
                    t.setStatus(TicketStatus.ASSIGNED);
                }
                notifications.ticketAssigned(t);
            }
        }
        tickets.save(t);

        s.setStatus(switch (req.decision()) { case ACCEPT -> SuggestionStatus.ACCEPTED; case PARTIAL -> SuggestionStatus.PARTIAL; case REJECT -> SuggestionStatus.REJECTED; });
        s.setReviewedBy(reviewer);
        s.setReviewedAt(Instant.now());
        s.setReviewComment(req.comment());
        audit.record(reviewer.getId(), "AI_SUGGESTION_REVIEWED", "AI_SUGGESTION", s.getId(), "decision=" + req.decision());
        return SuggestionResponse.from(suggestions.save(s));
    }

    @Transactional(readOnly = true)
    public List<SuggestionResponse> pending() {
        return suggestions.findByStatusOrderByCreatedAtDesc(SuggestionStatus.PENDING).stream().map(SuggestionResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<SuggestionResponse> forTicket(Long ticketId) {
        return suggestions.findByTicketIdOrderByCreatedAtDesc(ticketId).stream().map(SuggestionResponse::from).toList();
    }

    public AiDtos.AiStatus status() {
        return new AiDtos.AiStatus(aiAvailable(), ai.provider(), ai.model(), ai.dailyBudgetUsd(), spentToday(),
            suggestions.countByStatus(SuggestionStatus.PENDING), !aiAvailable() || budgetExceeded());
    }

    BigDecimal estimateCost(int inputTokens, int outputTokens) {
        BigDecimal million = BigDecimal.valueOf(1_000_000);
        BigDecimal in = ai.priceInputPerMtok().multiply(BigDecimal.valueOf(inputTokens)).divide(million, 6, RoundingMode.HALF_UP);
        BigDecimal out = ai.priceOutputPerMtok().multiply(BigDecimal.valueOf(outputTokens)).divide(million, 6, RoundingMode.HALF_UP);
        return in.add(out);
    }

    private static String truncate(String s, int max) { return s.length() > max ? s.substring(0, max) : s; }
}
