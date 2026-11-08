package sn.senetech.intervit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.senetech.intervit.domain.Site;

public interface SiteRepository extends JpaRepository<Site, Long> {
}
