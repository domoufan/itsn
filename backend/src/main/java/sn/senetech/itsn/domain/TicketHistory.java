package sn.senetech.itsn.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "ticket_history")
public class TicketHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "ticket_id") private Ticket ticket;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "changed_by_id") private User changedBy;
    @Column(nullable = false, length = 40) private String field;
    @Column(name = "old_value") private String oldValue;
    @Column(name = "new_value") private String newValue;
    @Column(name = "changed_at", nullable = false, updatable = false) private Instant changedAt = Instant.now();

    public static TicketHistory of(Ticket t, User by, String field, Object oldV, Object newV) {
        TicketHistory h = new TicketHistory();
        h.ticket = t; h.changedBy = by; h.field = field;
        h.oldValue = oldV == null ? null : String.valueOf(oldV);
        h.newValue = newV == null ? null : String.valueOf(newV);
        return h;
    }

    public Long getId() { return id; }
    public Ticket getTicket() { return ticket; }
    public User getChangedBy() { return changedBy; }
    public String getField() { return field; }
    public String getOldValue() { return oldValue; }
    public String getNewValue() { return newValue; }
    public Instant getChangedAt() { return changedAt; }
}
