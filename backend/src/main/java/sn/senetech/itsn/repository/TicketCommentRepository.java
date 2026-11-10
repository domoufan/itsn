package sn.senetech.itsn.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.senetech.itsn.domain.TicketComment;

import java.util.List;

public interface TicketCommentRepository extends JpaRepository<TicketComment, Long> {
    List<TicketComment> findByTicketIdOrderByCreatedAtAsc(Long ticketId);
    List<TicketComment> findByTicketIdAndInternalFalseOrderByCreatedAtAsc(Long ticketId);
}
