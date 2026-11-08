package sn.senetech.intervit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sn.senetech.intervit.domain.Intervention;

import java.time.Instant;
import java.util.List;

public interface InterventionRepository extends JpaRepository<Intervention, Long> {

    List<Intervention> findByTicketIdOrderByScheduledStartAsc(Long ticketId);

    @Query("select i from Intervention i where i.scheduledStart < :to and i.scheduledEnd > :from and i.status <> 'CANCELLED' order by i.scheduledStart")
    List<Intervention> findInWindow(@Param("from") Instant from, @Param("to") Instant to);

    @Query("select i from Intervention i where i.technician.id = :techId and i.scheduledStart < :to and i.scheduledEnd > :from and i.status <> 'CANCELLED' order by i.scheduledStart")
    List<Intervention> findByTechnicianInWindow(@Param("techId") Long techId, @Param("from") Instant from, @Param("to") Instant to);

    /** Détection de chevauchement de créneau pour un même technicien. */
    @Query("select count(i) from Intervention i where i.technician.id = :techId and i.status in ('PLANNED','IN_PROGRESS') and i.scheduledStart < :end and i.scheduledEnd > :start and (:excludeId is null or i.id <> :excludeId)")
    long countOverlapping(@Param("techId") Long techId, @Param("start") Instant start, @Param("end") Instant end, @Param("excludeId") Long excludeId);
}
