package sn.senetech.intervit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.senetech.intervit.domain.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByCode(String code);
    List<Category> findByActiveTrueOrderByName();
}
