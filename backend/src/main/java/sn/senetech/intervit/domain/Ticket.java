package sn.senetech.intervit.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "tickets")
public class Ticket {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 20) private String reference;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10) private TicketType type;
    @Column(nullable = false, length = 160) private String title;
    @Column(nullable = false, columnDefinition = "text") private String description;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "category_id") private Category category;
    @Column(nullable = false) private short impact;
    @Column(nullable = false) private short urgency;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 2) private Priority priority;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 15) private TicketStatus status = TicketStatus.NEW;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "requester_id") private User requester;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "assignee_id") private User assignee;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "site_id") private Site site;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "equipment_id") private Equipment equipment;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false) private Instant updatedAt = Instant.now();
    @Column(name = "response_due_at", nullable = false) private Instant responseDueAt;
    @Column(name = "resolution_due_at", nullable = false) private Instant resolutionDueAt;
    @Column(name = "first_response_at") private Instant firstResponseAt;
    @Column(name = "resolved_at") private Instant resolvedAt;
    @Column(name = "closed_at") private Instant closedAt;
    @Column(name = "sla_response_breached", nullable = false) private boolean slaResponseBreached;
    @Column(name = "sla_resolution_breached", nullable = false) private boolean slaResolutionBreached;
    @Column(name = "resolution_summary", columnDefinition = "text") private String resolutionSummary;
    @Column(name = "satisfaction_score") private Short satisfactionScore;

    @PreUpdate void onUpdate() { this.updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public TicketType getType() { return type; }
    public void setType(TicketType type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public short getImpact() { return impact; }
    public void setImpact(short impact) { this.impact = impact; }
    public short getUrgency() { return urgency; }
    public void setUrgency(short urgency) { this.urgency = urgency; }
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
    public TicketStatus getStatus() { return status; }
    public void setStatus(TicketStatus status) { this.status = status; }
    public User getRequester() { return requester; }
    public void setRequester(User requester) { this.requester = requester; }
    public User getAssignee() { return assignee; }
    public void setAssignee(User assignee) { this.assignee = assignee; }
    public Site getSite() { return site; }
    public void setSite(Site site) { this.site = site; }
    public Equipment getEquipment() { return equipment; }
    public void setEquipment(Equipment equipment) { this.equipment = equipment; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getResponseDueAt() { return responseDueAt; }
    public void setResponseDueAt(Instant responseDueAt) { this.responseDueAt = responseDueAt; }
    public Instant getResolutionDueAt() { return resolutionDueAt; }
    public void setResolutionDueAt(Instant resolutionDueAt) { this.resolutionDueAt = resolutionDueAt; }
    public Instant getFirstResponseAt() { return firstResponseAt; }
    public void setFirstResponseAt(Instant firstResponseAt) { this.firstResponseAt = firstResponseAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }
    public Instant getClosedAt() { return closedAt; }
    public void setClosedAt(Instant closedAt) { this.closedAt = closedAt; }
    public boolean isSlaResponseBreached() { return slaResponseBreached; }
    public void setSlaResponseBreached(boolean v) { this.slaResponseBreached = v; }
    public boolean isSlaResolutionBreached() { return slaResolutionBreached; }
    public void setSlaResolutionBreached(boolean v) { this.slaResolutionBreached = v; }
    public String getResolutionSummary() { return resolutionSummary; }
    public void setResolutionSummary(String resolutionSummary) { this.resolutionSummary = resolutionSummary; }
    public Short getSatisfactionScore() { return satisfactionScore; }
    public void setSatisfactionScore(Short satisfactionScore) { this.satisfactionScore = satisfactionScore; }
}
