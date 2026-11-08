package sn.senetech.intervit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sn.senetech.intervit.domain.AiSuggestion;
import sn.senetech.intervit.domain.SuggestionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface AiSuggestionRepository extends JpaRepository<AiSuggestion, Long> {
    List<AiSuggestion> findByStatusOrderByCreatedAtDesc(SuggestionStatus status);
    List<AiSuggestion> findByTicketIdOrderByCreatedAtDesc(Long ticketId);

    @Query("select coalesce(sum(s.estimatedCostUsd), 0) from AiSuggestion s where s.createdAt >= :since")
    BigDecimal sumCostSince(@Param("since") Instant since);

    long countByStatus(SuggestionStatus status);
}
