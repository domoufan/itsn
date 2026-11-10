package sn.senetech.itsn.catalog;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import sn.senetech.itsn.auth.CurrentUserService;
import sn.senetech.itsn.common.AuditService;
import sn.senetech.itsn.common.BusinessException;
import sn.senetech.itsn.common.NotFoundException;
import sn.senetech.itsn.domain.Role;
import sn.senetech.itsn.domain.User;
import sn.senetech.itsn.repository.SiteRepository;
import sn.senetech.itsn.repository.TicketRepository;
import sn.senetech.itsn.repository.UserRepository;
import sn.senetech.itsn.domain.TicketStatus;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;

/** Gestion des comptes (administrateur) et annuaire des techniciens (personnel). */
@RestController
@RequestMapping("/api/users")
public class UserAdminController {

    public record UserRequest(
            @NotBlank @Size(max = 120) String fullName,
            @NotBlank @Email String email,
            @Size(min = 10, max = 72) String password,
            @NotNull Role role,
            String phone,
            Long siteId,
            @Size(max = 255) String skills,
            Boolean active) {
    }

    public record UserResponse(Long id, String fullName, String email, Role role, String phone, Long siteId, String siteName,
                               String skills, boolean active, Instant createdAt, Instant lastLoginAt, Long openTickets) {
        static UserResponse from(User u, Long openTickets) {
            return new UserResponse(u.getId(), u.getFullName(), u.getEmail(), u.getRole(), u.getPhone(),
                u.getSite() == null ? null : u.getSite().getId(), u.getSite() == null ? null : u.getSite().getName(),
                u.getSkills(), u.isActive(), u.getCreatedAt(), u.getLastLoginAt(), openTickets);
        }
    }

    private static final EnumSet<TicketStatus> ACTIVE = EnumSet.of(TicketStatus.ASSIGNED, TicketStatus.IN_PROGRESS, TicketStatus.ON_HOLD);

    private final UserRepository users;
    private final SiteRepository sites;
    private final TicketRepository tickets;
    private final PasswordEncoder encoder;
    private final CurrentUserService currentUser;
    private final AuditService audit;

    public UserAdminController(UserRepository users, SiteRepository sites, TicketRepository tickets, PasswordEncoder encoder,
                               CurrentUserService currentUser, AuditService audit) {
        this.users = users; this.sites = sites; this.tickets = tickets; this.encoder = encoder; this.currentUser = currentUser; this.audit = audit;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public List<UserResponse> list(@RequestParam(required = false) Role role) {
        List<User> list = role == null ? users.findAll() : users.findByRoleAndActiveTrueOrderByFullName(role);
        return list.stream().map(u -> UserResponse.from(u, u.getRole() == Role.TECHNICIAN ? tickets.countByAssigneeIdAndStatusIn(u.getId(), ACTIVE) : null)).toList();
    }

    /** Liste des techniciens actifs avec leur charge courante (utilisée pour l'affectation et par l'agent IA). */
    @GetMapping("/technicians")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','TECHNICIAN')")
    public List<UserResponse> technicians() {
        return users.findByRoleAndActiveTrueOrderByFullName(Role.TECHNICIAN).stream()
            .map(u -> UserResponse.from(u, tickets.countByAssigneeIdAndStatusIn(u.getId(), ACTIVE))).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public UserResponse create(@Valid @RequestBody UserRequest req) {
        if (users.existsByEmailIgnoreCase(req.email())) throw new BusinessException("Un compte existe déjà avec cet e-mail");
        if (req.password() == null) throw new BusinessException("Mot de passe initial requis");
        User u = new User();
        apply(u, req);
        u.setPasswordHash(encoder.encode(req.password()));
        u = users.save(u);
        audit.record(currentUser.currentUserId(), "USER_CREATED", "USER", u.getId(), "role=" + u.getRole());
        return UserResponse.from(u, null);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UserRequest req) {
        User u = users.findById(id).orElseThrow(() -> new NotFoundException("Utilisateur", id));
        users.findByEmailIgnoreCase(req.email()).filter(o -> !o.getId().equals(id))
            .ifPresent(o -> { throw new BusinessException("E-mail déjà utilisé par un autre compte"); });
        Role before = u.getRole();
        apply(u, req);
        if (req.password() != null && !req.password().isBlank()) u.setPasswordHash(encoder.encode(req.password()));
        if (before != u.getRole()) audit.record(currentUser.currentUserId(), "USER_ROLE_CHANGED", "USER", id, before + "->" + u.getRole());
        return UserResponse.from(users.save(u), null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void deactivate(@PathVariable Long id) {
        User u = users.findById(id).orElseThrow(() -> new NotFoundException("Utilisateur", id));
        if (u.getId().equals(currentUser.currentUserId())) throw new BusinessException("Impossible de désactiver son propre compte");
        u.setActive(false); // désactivation logique : l'historique des tickets est conservé
        users.save(u);
        audit.record(currentUser.currentUserId(), "USER_DEACTIVATED", "USER", id, null);
    }

    private void apply(User u, UserRequest req) {
        u.setFullName(req.fullName().trim());
        u.setEmail(req.email().trim().toLowerCase());
        u.setRole(req.role());
        u.setPhone(req.phone());
        u.setSkills(req.skills());
        u.setSite(req.siteId() == null ? null : sites.findById(req.siteId()).orElseThrow(() -> new NotFoundException("Site", req.siteId())));
        if (req.active() != null) u.setActive(req.active());
    }
}
