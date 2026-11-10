package sn.senetech.itsn.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.senetech.itsn.domain.Role;
import sn.senetech.itsn.domain.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    List<User> findByRoleAndActiveTrueOrderByFullName(Role role);
    long countByRole(Role role);
}
