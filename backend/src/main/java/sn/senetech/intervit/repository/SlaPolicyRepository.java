package sn.senetech.intervit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.senetech.intervit.domain.Priority;
import sn.senetech.intervit.domain.SlaPolicy;

import java.util.Optional;

public interface SlaPolicyRepository extends JpaRepository<SlaPolicy, Long> {
    Optional<SlaPolicy> findByPriority(Priority priority);
}
