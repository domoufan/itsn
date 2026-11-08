package sn.senetech.intervit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.senetech.intervit.domain.Equipment;

import java.util.List;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {
    boolean existsByAssetTag(String assetTag);
    List<Equipment> findBySiteIdOrderByAssetTag(Long siteId);
}
