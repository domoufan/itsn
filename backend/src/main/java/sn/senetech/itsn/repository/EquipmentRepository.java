package sn.senetech.itsn.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.senetech.itsn.domain.Equipment;

import java.util.List;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {
    boolean existsByAssetTag(String assetTag);
    List<Equipment> findBySiteIdOrderByAssetTag(Long siteId);
}
