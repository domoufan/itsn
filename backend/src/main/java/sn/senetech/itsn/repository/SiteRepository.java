package sn.senetech.itsn.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.senetech.itsn.domain.Site;

public interface SiteRepository extends JpaRepository<Site, Long> {
}
