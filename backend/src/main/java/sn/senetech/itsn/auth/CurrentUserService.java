package sn.senetech.itsn.auth;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import sn.senetech.itsn.common.NotFoundException;
import sn.senetech.itsn.domain.Role;
import sn.senetech.itsn.domain.User;
import sn.senetech.itsn.repository.UserRepository;

/** Résout l'utilisateur courant à partir du JWT porté par la requête. */
@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            return Long.valueOf(jwt.getSubject());
        }
        throw new IllegalStateException("Aucun utilisateur authentifié");
    }

    public User currentUser() {
        Long id = currentUserId();
        return userRepository.findById(id).orElseThrow(() -> new NotFoundException("Utilisateur", id));
    }

    public boolean hasRole(Role role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_" + role.name()));
    }

    public boolean isStaff() {
        return hasRole(Role.ADMIN) || hasRole(Role.MANAGER) || hasRole(Role.TECHNICIAN);
    }
}
