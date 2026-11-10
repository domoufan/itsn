package sn.senetech.itsn.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.senetech.itsn.domain.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByCode(String code);
    List<Category> findByActiveTrueOrderByName();
}
