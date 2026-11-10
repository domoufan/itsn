package sn.senetech.itsn.ticket;

import jakarta.validation.constraints.*;
import sn.senetech.itsn.domain.*;

import java.time.Instant;
import java.util.List;

/** Objets d'échange de l'API tickets (requêtes validées et réponses). */
public final class TicketDtos {
    private TicketDtos() {}

    public record CreateRequest(
            @NotNull TicketType type,
            @NotBlank @Size(min = 5, max = 160) String title,
            @NotBlank @Size(min = 10, max = 5000) String description,
            Long categoryId,
            @NotNull @Min(1) @Max(3) Integer impact,
            @NotNull @Min(1) @Max(3) Integer urgency,
            Long siteId,
            Long equipmentId,
            /** Réservé au personnel : créer un ticket pour le compte d'un demandeur. */
            Long requesterId) {
    }

    public record UpdateRequest(
            @Size(min = 5, max = 160) String title,
            @Size(min = 10, max = 5000) String description,
            Long categoryId,
            @Min(1) @Max(3) Integer impact,
            @Min(1) @Max(3) Integer urgency,
            Long siteId,
            Long equipmentId) {
    }

    public record AssignRequest(@NotNull Long assigneeId, @Size(max = 500) String comment) {
    }

    public record StatusChangeRequest(
            @NotNull TicketStatus status,
            @Size(max = 2000) String comment,
            @Size(max = 5000) String resolutionSummary) {
    }

    public record CommentRequest(@NotBlank @Size(max = 5000) String content, Boolean internal) {
        public boolean isInternal() { return Boolean.TRUE.equals(internal); }
    }

    public record SatisfactionRequest(@NotNull @Min(1) @Max(5) Integer score) {
    }

    public record UserRef(Long id, String fullName, String email) {
        public static UserRef from(User u) { return u == null ? null : new UserRef(u.getId(), u.getFullName(), u.getEmail()); }
    }

    public record CategoryRef(Long id, String code, String name) {
        public static CategoryRef from(Category c) { return c == null ? null : new CategoryRef(c.getId(), c.getCode(), c.getName()); }
    }

    public record SiteRef(Long id, String name, String clientName) {
        public static SiteRef from(Site s) { return s == null ? null : new SiteRef(s.getId(), s.getName(), s.getClientName()); }
    }

    public record EquipmentRef(Long id, String assetTag, EquipmentType type, String brand, String model) {
        public static EquipmentRef from(Equipment e) { return e == null ? null : new EquipmentRef(e.getId(), e.getAssetTag(), e.getType(), e.getBrand(), e.getModel()); }
    }

    public record TicketResponse(
            Long id, String reference, TicketType type, String title, String description,
            CategoryRef category, int impact, int urgency, Priority priority, TicketStatus status,
            UserRef requester, UserRef assignee, SiteRef site, EquipmentRef equipment,
            Instant createdAt, Instant updatedAt, Instant responseDueAt, Instant resolutionDueAt,
            Instant firstResponseAt, Instant resolvedAt, Instant closedAt,
            boolean slaResponseBreached, boolean slaResolutionBreached,
            String resolutionSummary, Short satisfactionScore, List<TicketStatus> allowedTransitions) {

        public static TicketResponse from(Ticket t) {
            List<TicketStatus> allowed = java.util.Arrays.stream(TicketStatus.values())
                .filter(s -> t.getStatus().canTransitionTo(s) && s != t.getStatus()).toList();
            return new TicketResponse(t.getId(), t.getReference(), t.getType(), t.getTitle(), t.getDescription(),
                CategoryRef.from(t.getCategory()), t.getImpact(), t.getUrgency(), t.getPriority(), t.getStatus(),
                UserRef.from(t.getRequester()), UserRef.from(t.getAssignee()), SiteRef.from(t.getSite()), EquipmentRef.from(t.getEquipment()),
                t.getCreatedAt(), t.getUpdatedAt(), t.getResponseDueAt(), t.getResolutionDueAt(),
                t.getFirstResponseAt(), t.getResolvedAt(), t.getClosedAt(),
                t.isSlaResponseBreached(), t.isSlaResolutionBreached(),
                t.getResolutionSummary(), t.getSatisfactionScore(), allowed);
        }
    }

    public record TicketSummary(
            Long id, String reference, TicketType type, String title, CategoryRef category, Priority priority,
            TicketStatus status, UserRef requester, UserRef assignee, SiteRef site, Instant createdAt,
            Instant resolutionDueAt, boolean slaResponseBreached, boolean slaResolutionBreached) {
        public static TicketSummary from(Ticket t) {
            return new TicketSummary(t.getId(), t.getReference(), t.getType(), t.getTitle(), CategoryRef.from(t.getCategory()),
                t.getPriority(), t.getStatus(), UserRef.from(t.getRequester()), UserRef.from(t.getAssignee()), SiteRef.from(t.getSite()),
                t.getCreatedAt(), t.getResolutionDueAt(), t.isSlaResponseBreached(), t.isSlaResolutionBreached());
        }
    }

    public record CommentResponse(Long id, UserRef author, String content, boolean internal, Instant createdAt) {
        public static CommentResponse from(TicketComment c) {
            return new CommentResponse(c.getId(), UserRef.from(c.getAuthor()), c.getContent(), c.isInternal(), c.getCreatedAt());
        }
    }

    public record HistoryResponse(Long id, UserRef changedBy, String field, String oldValue, String newValue, Instant changedAt) {
        public static HistoryResponse from(TicketHistory h) {
            return new HistoryResponse(h.getId(), UserRef.from(h.getChangedBy()), h.getField(), h.getOldValue(), h.getNewValue(), h.getChangedAt());
        }
    }

    public record TicketFilter(TicketStatus status, Priority priority, TicketType type, Long assigneeId, Long requesterId,
                               Long siteId, Long categoryId, Boolean openOnly, Boolean slaBreached, String search) {
    }
}
