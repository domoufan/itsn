package sn.senetech.itsn.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "sla_policies")
public class SlaPolicy {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING) @Column(nullable = false, unique = true, length = 2) private Priority priority;
    @Column(nullable = false, length = 40) private String label;
    @Column(name = "response_minutes", nullable = false) private int responseMinutes;
    @Column(name = "resolution_minutes", nullable = false) private int resolutionMinutes;

    public Long getId() { return id; }
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public int getResponseMinutes() { return responseMinutes; }
    public void setResponseMinutes(int responseMinutes) { this.responseMinutes = responseMinutes; }
    public int getResolutionMinutes() { return resolutionMinutes; }
    public void setResolutionMinutes(int resolutionMinutes) { this.resolutionMinutes = resolutionMinutes; }
}
