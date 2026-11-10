package sn.senetech.itsn.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "interventions")
public class Intervention {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "ticket_id") private Ticket ticket;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "technician_id") private User technician;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10) private InterventionMode mode;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 12) private InterventionStatus status = InterventionStatus.PLANNED;
    @Column(name = "scheduled_start", nullable = false) private Instant scheduledStart;
    @Column(name = "scheduled_end", nullable = false) private Instant scheduledEnd;
    @Column(name = "actual_start") private Instant actualStart;
    @Column(name = "actual_end") private Instant actualEnd;
    @Column(columnDefinition = "text") private String report;
    @Column(name = "parts_used") private String partsUsed;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public Ticket getTicket() { return ticket; }
    public void setTicket(Ticket ticket) { this.ticket = ticket; }
    public User getTechnician() { return technician; }
    public void setTechnician(User technician) { this.technician = technician; }
    public InterventionMode getMode() { return mode; }
    public void setMode(InterventionMode mode) { this.mode = mode; }
    public InterventionStatus getStatus() { return status; }
    public void setStatus(InterventionStatus status) { this.status = status; }
    public Instant getScheduledStart() { return scheduledStart; }
    public void setScheduledStart(Instant scheduledStart) { this.scheduledStart = scheduledStart; }
    public Instant getScheduledEnd() { return scheduledEnd; }
    public void setScheduledEnd(Instant scheduledEnd) { this.scheduledEnd = scheduledEnd; }
    public Instant getActualStart() { return actualStart; }
    public void setActualStart(Instant actualStart) { this.actualStart = actualStart; }
    public Instant getActualEnd() { return actualEnd; }
    public void setActualEnd(Instant actualEnd) { this.actualEnd = actualEnd; }
    public String getReport() { return report; }
    public void setReport(String report) { this.report = report; }
    public String getPartsUsed() { return partsUsed; }
    public void setPartsUsed(String partsUsed) { this.partsUsed = partsUsed; }
    public Instant getCreatedAt() { return createdAt; }
}
