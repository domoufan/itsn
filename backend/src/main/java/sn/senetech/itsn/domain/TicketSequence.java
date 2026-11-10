package sn.senetech.itsn.domain;

import jakarta.persistence.*;

/** Compteur annuel pour générer des références lisibles (INC-2026-00042). */
@Entity
@Table(name = "ticket_sequences")
public class TicketSequence {
    @Id private Integer year;
    @Column(name = "last_value", nullable = false) private int lastValue;

    protected TicketSequence() {}
    public TicketSequence(int year) { this.year = year; this.lastValue = 0; }
    public Integer getYear() { return year; }
    public int getLastValue() { return lastValue; }
    public int next() { return ++lastValue; }
}
