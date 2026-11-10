package sn.senetech.itsn.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "audit_log")
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "actor_id") private Long actorId;
    @Column(nullable = false, length = 60) private String action;
    @Column(name = "target_type", length = 40) private String targetType;
    @Column(name = "target_id") private Long targetId;
    @Column(length = 1000) private String details;
    @Column(name = "ip_address", length = 45) private String ipAddress;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt = Instant.now();

    public static AuditLog of(Long actorId, String action, String targetType, Long targetId, String details, String ip) {
        AuditLog a = new AuditLog();
        a.actorId = actorId; a.action = action; a.targetType = targetType; a.targetId = targetId;
        a.details = details == null ? null : (details.length() > 1000 ? details.substring(0, 1000) : details);
        a.ipAddress = ip;
        return a;
    }

    public Long getId() { return id; }
    public Long getActorId() { return actorId; }
    public String getAction() { return action; }
    public String getTargetType() { return targetType; }
    public Long getTargetId() { return targetId; }
    public String getDetails() { return details; }
    public String getIpAddress() { return ipAddress; }
    public Instant getCreatedAt() { return createdAt; }
}
