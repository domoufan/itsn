package sn.senetech.itsn.ai;

import jakarta.validation.constraints.*;
import sn.senetech.itsn.domain.AiSuggestion;
import sn.senetech.itsn.domain.Priority;
import sn.senetech.itsn.domain.SuggestionStatus;

import java.math.BigDecimal;
import java.time.Instant;

public final class AiDtos {
    private AiDtos() {}

    /** Décision du responsable : accepter tout, accepter certains champs, ou rejeter. */
    public record ReviewRequest(
            @NotNull Decision decision,
            Boolean applyCategory,
            Boolean applyPriority,
            Boolean applyAssignee,
            @Size(max = 500) String comment) {
        public enum Decision { ACCEPT, PARTIAL, REJECT }
        public boolean category() { return Boolean.TRUE.equals(applyCategory); }
        public boolean priority() { return Boolean.TRUE.equals(applyPriority); }
        public boolean assignee() { return Boolean.TRUE.equals(applyAssignee); }
    }

    public record SuggestionResponse(
            Long id, Long ticketId, String ticketReference, String ticketTitle, SuggestionStatus status,
            Long suggestedCategoryId, String suggestedCategoryCode, String suggestedCategoryName,
            Short suggestedImpact, Short suggestedUrgency, Priority suggestedPriority,
            Long suggestedAssigneeId, String suggestedAssigneeName,
            String summary, String rationale, String relatedArticleIds,
            String model, int toolCalls, int inputTokens, int outputTokens, int latencyMs, BigDecimal estimatedCostUsd,
            String errorMessage, Instant createdAt, String reviewedByName, Instant reviewedAt, String reviewComment,
            // valeurs actuelles du ticket, pour comparaison dans l'écran de revue
            String currentCategoryCode, Priority currentPriority, String currentAssigneeName) {

        public static SuggestionResponse from(AiSuggestion s) {
            var t = s.getTicket();
            return new SuggestionResponse(s.getId(), t.getId(), t.getReference(), t.getTitle(), s.getStatus(),
                s.getSuggestedCategory() == null ? null : s.getSuggestedCategory().getId(),
                s.getSuggestedCategory() == null ? null : s.getSuggestedCategory().getCode(),
                s.getSuggestedCategory() == null ? null : s.getSuggestedCategory().getName(),
                s.getSuggestedImpact(), s.getSuggestedUrgency(), s.getSuggestedPriority(),
                s.getSuggestedAssignee() == null ? null : s.getSuggestedAssignee().getId(),
                s.getSuggestedAssignee() == null ? null : s.getSuggestedAssignee().getFullName(),
                s.getSummary(), s.getRationale(), s.getRelatedArticleIds(),
                s.getModel(), s.getToolCalls(), s.getInputTokens(), s.getOutputTokens(), s.getLatencyMs(), s.getEstimatedCostUsd(),
                s.getErrorMessage(), s.getCreatedAt(),
                s.getReviewedBy() == null ? null : s.getReviewedBy().getFullName(), s.getReviewedAt(), s.getReviewComment(),
                t.getCategory() == null ? null : t.getCategory().getCode(), t.getPriority(),
                t.getAssignee() == null ? null : t.getAssignee().getFullName());
        }
    }

    public record AiStatus(boolean enabled, String provider, String model, BigDecimal dailyBudgetUsd, BigDecimal spentTodayUsd,
                           long pendingSuggestions, boolean fallbackActive) {}
}
