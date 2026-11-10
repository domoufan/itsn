package sn.senetech.itsn.intervention;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import sn.senetech.itsn.auth.CurrentUserService;
import sn.senetech.itsn.common.BusinessException;
import sn.senetech.itsn.common.NotFoundException;
import sn.senetech.itsn.domain.*;
import sn.senetech.itsn.repository.InterventionRepository;
import sn.senetech.itsn.repository.TicketRepository;
import sn.senetech.itsn.repository.UserRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/** Planification et compte rendu des interventions (sur site ou à distance) rattachées à un ticket. */
@RestController
@RequestMapping("/api/interventions")
public class InterventionController {

    public record PlanRequest(@NotNull Long ticketId, @NotNull Long technicianId, @NotNull InterventionMode mode,
                              @NotNull Instant scheduledStart, @NotNull Instant scheduledEnd) {}

    public record ReportRequest(@NotNull InterventionStatus status, Instant actualStart, Instant actualEnd,
                                @Size(max = 5000) String report, @Size(max = 500) String partsUsed) {}

    public record InterventionResponse(Long id, Long ticketId, String ticketReference, String ticketTitle, Priority ticketPriority,
                                       Long technicianId, String technicianName, InterventionMode mode, InterventionStatus status,
                                       Instant scheduledStart, Instant scheduledEnd, Instant actualStart, Instant actualEnd,
                                       String report, String partsUsed, String siteName) {
        static InterventionResponse from(Intervention i) {
            Ticket t = i.getTicket();
            return new InterventionResponse(i.getId(), t.getId(), t.getReference(), t.getTitle(), t.getPriority(),
                i.getTechnician().getId(), i.getTechnician().getFullName(), i.getMode(), i.getStatus(),
                i.getScheduledStart(), i.getScheduledEnd(), i.getActualStart(), i.getActualEnd(), i.getReport(), i.getPartsUsed(),
                t.getSite() == null ? null : t.getSite().getName());
        }
    }

    private final InterventionRepository interventions;
    private final TicketRepository tickets;
    private final UserRepository users;
    private final CurrentUserService currentUser;

    public InterventionController(InterventionRepository interventions, TicketRepository tickets, UserRepository users, CurrentUserService currentUser) {
        this.interventions = interventions; this.tickets = tickets; this.users = users; this.currentUser = currentUser;
    }

    /** Planning sur une fenêtre temporelle (par défaut : 7 jours glissants), filtrable par technicien. */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','TECHNICIAN')")
    public List<InterventionResponse> planning(@RequestParam(required = false) Instant from, @RequestParam(required = false) Instant to,
                                               @RequestParam(required = false) Long technicianId) {
        Instant f = from == null ? Instant.now().minus(Duration.ofDays(1)) : from;
        Instant t = to == null ? f.plus(Duration.ofDays(7)) : to;
        if (Duration.between(f, t).toDays() > 62) throw new IllegalArgumentException("Fenêtre de planning limitée à 62 jours");
        List<Intervention> list = technicianId == null ? interventions.findInWindow(f, t) : interventions.findByTechnicianInWindow(technicianId, f, t);
        return list.stream().map(InterventionResponse::from).toList();
    }

    @GetMapping("/ticket/{ticketId}")
    public List<InterventionResponse> byTicket(@PathVariable Long ticketId) {
        Ticket t = tickets.findById(ticketId).orElseThrow(() -> new NotFoundException("Ticket", ticketId));
        User me = currentUser.currentUser();
        if (me.getRole() == Role.REQUESTER && !t.getRequester().getId().equals(me.getId())) throw new AccessDeniedException("Accès refusé");
        return interventions.findByTicketIdOrderByScheduledStartAsc(ticketId).stream().map(InterventionResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','TECHNICIAN')")
    @Transactional
    public InterventionResponse plan(@Valid @RequestBody PlanRequest req) {
        if (!req.scheduledEnd().isAfter(req.scheduledStart())) throw new BusinessException("La fin doit être postérieure au début");
        Ticket t = tickets.findById(req.ticketId()).orElseThrow(() -> new NotFoundException("Ticket", req.ticketId()));
        if (t.getStatus().isTerminal() || t.getStatus() == TicketStatus.RESOLVED) throw new BusinessException("Aucune intervention ne peut être planifiée sur un ticket résolu ou clos");
        User tech = users.findById(req.technicianId()).orElseThrow(() -> new NotFoundException("Utilisateur", req.technicianId()));
        if (tech.getRole() != Role.TECHNICIAN) throw new BusinessException("L'intervenant doit être un technicien");
        User me = currentUser.currentUser();
        if (me.getRole() == Role.TECHNICIAN && !tech.getId().equals(me.getId())) throw new AccessDeniedException("Un technicien ne planifie que ses propres interventions");
        if (interventions.countOverlapping(tech.getId(), req.scheduledStart(), req.scheduledEnd(), null) > 0) {
            throw new BusinessException("Conflit de planning : le technicien a déjà une intervention sur ce créneau");
        }
        Intervention i = new Intervention();
        i.setTicket(t); i.setTechnician(tech); i.setMode(req.mode());
        i.setScheduledStart(req.scheduledStart()); i.setScheduledEnd(req.scheduledEnd());
        return InterventionResponse.from(interventions.save(i));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','TECHNICIAN')")
    @Transactional
    public InterventionResponse report(@PathVariable Long id, @Valid @RequestBody ReportRequest req) {
        Intervention i = interventions.findById(id).orElseThrow(() -> new NotFoundException("Intervention", id));
        User me = currentUser.currentUser();
        if (me.getRole() == Role.TECHNICIAN && !i.getTechnician().getId().equals(me.getId())) throw new AccessDeniedException("Intervention d'un autre technicien");
        if (i.getStatus() == InterventionStatus.DONE || i.getStatus() == InterventionStatus.CANCELLED) throw new BusinessException("Intervention déjà terminée");
        if (req.status() == InterventionStatus.DONE && (req.report() == null || req.report().isBlank())) throw new BusinessException("Un compte rendu est obligatoire pour clore une intervention");
        if (req.actualStart() != null && req.actualEnd() != null && !req.actualEnd().isAfter(req.actualStart())) throw new BusinessException("Horaires réels incohérents");
        i.setStatus(req.status());
        i.setActualStart(req.actualStart()); i.setActualEnd(req.actualEnd());
        i.setReport(req.report()); i.setPartsUsed(req.partsUsed());
        return InterventionResponse.from(interventions.save(i));
    }
}
