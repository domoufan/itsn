package sn.senetech.intervit.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sn.senetech.intervit.domain.TicketSequence;

import java.util.Optional;

public interface TicketSequenceRepository extends JpaRepository<TicketSequence, Integer> {
    /** Verrou pessimiste : deux créations simultanées ne produisent jamais la même référence. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from TicketSequence s where s.year = :year")
    Optional<TicketSequence> lockByYear(@Param("year") int year);
}
