package sn.senetech.intervit.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sn.senetech.intervit.domain.Ticket;
import sn.senetech.intervit.domain.TicketStatus;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

    Optional<Ticket> findByReference(String reference);

    long countByStatusIn(Collection<TicketStatus> statuses);

    long countByCreatedAtBetween(Instant from, Instant to);

    long countByStatusInAndResolutionDueAtBefore(Collection<TicketStatus> statuses, Instant now);

    long countByAssigneeIdAndStatusIn(Long assigneeId, Collection<TicketStatus> statuses);

    @Query("select t from Ticket t where t.status in :statuses and t.firstResponseAt is null and t.responseDueAt < :now and t.slaResponseBreached = false")
    List<Ticket> findResponseBreaches(@Param("statuses") Collection<TicketStatus> statuses, @Param("now") Instant now);

    @Query("select t from Ticket t where t.status in :statuses and t.resolutionDueAt < :now and t.slaResolutionBreached = false")
    List<Ticket> findResolutionBreaches(@Param("statuses") Collection<TicketStatus> statuses, @Param("now") Instant now);

    @Query("select t from Ticket t where t.resolvedAt is not null and t.resolvedAt between :from and :to")
    List<Ticket> findResolvedBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("select t.priority, count(t) from Ticket t where t.status in :statuses group by t.priority")
    List<Object[]> countOpenByPriority(@Param("statuses") Collection<TicketStatus> statuses);

    @Query("select t.category.name, count(t) from Ticket t where t.createdAt >= :from group by t.category.name order by count(t) desc")
    List<Object[]> countByCategorySince(@Param("from") Instant from);

    @Query("select t.assignee.id, t.assignee.fullName, count(t) from Ticket t where t.assignee is not null and t.status in :statuses group by t.assignee.id, t.assignee.fullName")
    List<Object[]> workloadByTechnician(@Param("statuses") Collection<TicketStatus> statuses);

    /** Recherche plein texte simplifiée pour l'outil "tickets similaires" de l'agent. */
    @Query("select t from Ticket t where t.status in ('RESOLVED','CLOSED') and (lower(t.title) like lower(concat('%', :term, '%')) or lower(t.description) like lower(concat('%', :term, '%'))) order by t.resolvedAt desc")
    Page<Ticket> searchResolved(@Param("term") String term, Pageable pageable);
}
