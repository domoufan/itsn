package sn.senetech.itsn.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

/** Proposition produite par l'agent de triage. Jamais appliquée sans validation humaine. */
@Entity
@Table(name = "ai_suggestions")
public class AiSuggestion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "ticket_id") private Ticket ticket;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 12) private SuggestionStatus status = SuggestionStatus.PENDING;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "suggested_category_id") private Category suggestedCategory;
    @Column(name = "suggested_impact") private Short suggestedImpact;
    @Column(name = "suggested_urgency") private Short suggestedUrgency;
    @Enumerated(EnumType.STRING) @Column(name = "suggested_priority", length = 2) private Priority suggestedPriority;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "suggested_assignee_id") private User suggestedAssignee;
    @Column(columnDefinition = "text") private String summary;
    @Column(columnDefinition = "text") private String rationale;
    @Column(name = "related_article_ids") private String relatedArticleIds;
    private String model;
    @Column(name = "tool_calls", nullable = false) private int toolCalls;
    @Column(name = "input_tokens", nullable = false) private int inputTokens;
    @Column(name = "output_tokens", nullable = false) private int outputTokens;
    @Column(name = "latency_ms", nullable = false) private int latencyMs;
    @Column(name = "estimated_cost_usd", nullable = false, precision = 10, scale = 6) private BigDecimal estimatedCostUsd = BigDecimal.ZERO;
    @Column(name = "error_message") private String errorMessage;
    @Column(name = "raw_response", columnDefinition = "text") private String rawResponse;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt = Instant.now();
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "reviewed_by_id") private User reviewedBy;
    @Column(name = "reviewed_at") private Instant reviewedAt;
    @Column(name = "review_comment") private String reviewComment;

    public Long getId() { return id; }
    public Ticket getTicket() { return ticket; }
    public void setTicket(Ticket ticket) { this.ticket = ticket; }
    public SuggestionStatus getStatus() { return status; }
    public void setStatus(SuggestionStatus status) { this.status = status; }
    public Category getSuggestedCategory() { return suggestedCategory; }
    public void setSuggestedCategory(Category c) { this.suggestedCategory = c; }
    public Short getSuggestedImpact() { return suggestedImpact; }
    public void setSuggestedImpact(Short v) { this.suggestedImpact = v; }
    public Short getSuggestedUrgency() { return suggestedUrgency; }
    public void setSuggestedUrgency(Short v) { this.suggestedUrgency = v; }
    public Priority getSuggestedPriority() { return suggestedPriority; }
    public void setSuggestedPriority(Priority p) { this.suggestedPriority = p; }
    public User getSuggestedAssignee() { return suggestedAssignee; }
    public void setSuggestedAssignee(User u) { this.suggestedAssignee = u; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getRationale() { return rationale; }
    public void setRationale(String rationale) { this.rationale = rationale; }
    public String getRelatedArticleIds() { return relatedArticleIds; }
    public void setRelatedArticleIds(String v) { this.relatedArticleIds = v; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public int getToolCalls() { return toolCalls; }
    public void setToolCalls(int toolCalls) { this.toolCalls = toolCalls; }
    public int getInputTokens() { return inputTokens; }
    public void setInputTokens(int inputTokens) { this.inputTokens = inputTokens; }
    public int getOutputTokens() { return outputTokens; }
    public void setOutputTokens(int outputTokens) { this.outputTokens = outputTokens; }
    public int getLatencyMs() { return latencyMs; }
    public void setLatencyMs(int latencyMs) { this.latencyMs = latencyMs; }
    public BigDecimal getEstimatedCostUsd() { return estimatedCostUsd; }
    public void setEstimatedCostUsd(BigDecimal v) { this.estimatedCostUsd = v; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getRawResponse() { return rawResponse; }
    public void setRawResponse(String rawResponse) { this.rawResponse = rawResponse; }
    public Instant getCreatedAt() { return createdAt; }
    public User getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(User reviewedBy) { this.reviewedBy = reviewedBy; }
    public Instant getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(Instant reviewedAt) { this.reviewedAt = reviewedAt; }
    public String getReviewComment() { return reviewComment; }
    public void setReviewComment(String reviewComment) { this.reviewComment = reviewComment; }
}
